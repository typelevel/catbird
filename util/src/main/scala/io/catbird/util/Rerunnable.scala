package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, MonadError, Monoid, Semigroup }
import com.twitter.util.{ Await, Duration, Future, FuturePool, Return, Throw, Try }
import java.lang.Throwable
import scala.Unit
import scala.annotation.tailrec
import scala.util.{ Either, Left, Right }

abstract class Rerunnable[+A] { self =>
  def run: Future[A]

  final def map[B](f: A => B): Rerunnable[B] = new Rerunnable.Bind[B] {
    type P = A

    final def fa: Rerunnable[A] = self
    final def ff: Try[A] => Rerunnable[B] = {
      case Return(a)    => Rerunnable.const[B](f(a))
      case Throw(error) => Rerunnable.raiseError[B](error)
    }
  }

  final def flatMap[B](f: A => Rerunnable[B]): Rerunnable[B] = new Rerunnable.Bind[B] {
    type P = A

    final def fa: Rerunnable[A] = self
    final def ff: Try[A] => Rerunnable[B] = {
      case Return(a)    => f(a)
      case Throw(error) => Rerunnable.raiseError[B](error)
    }
  }

  final def flatMapF[B](f: A => Future[B]): Rerunnable[B] = new Rerunnable[B] {
    final def run: Future[B] = self.run.flatMap(f)
  }

  final def product[B](other: Rerunnable[B]): Rerunnable[(A, B)] = new Rerunnable.Bind[(A, B)] {
    type P = A

    final def fa: Rerunnable[A] = self
    final def ff: Try[A] => Rerunnable[(A, B)] = {
      case Return(a) =>
        new Rerunnable.Bind[(A, B)] {
          type P = B

          final def fa: Rerunnable[B] = other
          final def ff: Try[B] => Rerunnable[(A, B)] = {
            case Return(b)    => Rerunnable.const((a, b))
            case Throw(error) => Rerunnable.raiseError[(A, B)](error)
          }
        }
      case Throw(error) => Rerunnable.raiseError[(A, B)](error)
    }
  }

  final def liftToTry: Rerunnable[Try[A]] = new Rerunnable.Bind[Try[A]] {
    type P = A
    final def fa: Rerunnable[A] = self
    final def ff: Try[A] => Rerunnable[Try[A]] = Rerunnable.const(_)
  }
}

final object Rerunnable extends RerunnableInstances1 {
  @tailrec
  private[this] def reassociate[B](bind: Bind[B]): Bind[B] = {
    if (bind.fa.isInstanceOf[Bind[_]]) {
      val inner = bind.fa.asInstanceOf[Bind[bind.P]]
      val next = new Bind[B] {
        final type P = inner.P

        final def fa: Rerunnable[P] = inner.fa
        final def ff: Try[P] => Rerunnable[B] = p =>
          new Bind[B] {
            final type P = bind.P

            final val fa: Rerunnable[P] = inner.ff(p)
            final val ff: Try[P] => Rerunnable[B] = bind.ff
          }
      }

      reassociate(next)
    } else bind
  }

  private[util] abstract class Bind[B] extends Rerunnable[B] { self =>
    type P

    def fa: Rerunnable[P]
    def ff: Try[P] => Rerunnable[B]

    private[this] lazy val next = reassociate[B](this)
    final def run: Future[B] = {
      val localff = next.ff // don't close over this or next in closure
      next.fa.run.transform(t => localff(t).run)
    }
  }

  private[util] case class ConstRerunnable[A](run: Future[A]) extends Rerunnable[A]

  final def const[A](a: A): Rerunnable[A] =
    ConstRerunnable(Future.value(a))

  final def raiseError[A](error: Throwable): Rerunnable[A] =
    ConstRerunnable(Future.exception[A](error))

  final def apply[A](a: => A): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = Future(a)
  }

  final def suspend[A](fa: => Rerunnable[A]): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = Future(fa).flatMap(_.run)
  }

  final def fromFuture[A](fa: => Future[A]): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = fa
  }

  final def withFuturePool[A](pool: FuturePool)(a: => A): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = pool(a)
  }

  final val Unit: Rerunnable[Unit] =
    ConstRerunnable(Future.Unit)

  implicit final val rerunnableInstance: MonadError[Rerunnable, Throwable] with CoflatMap[Rerunnable] =
    new RerunnableMonadError with RerunnableCoflatMap

  implicit final def rerunnableMonoid[A](implicit A: Monoid[A]): Monoid[Rerunnable[A]] =
    new RerunnableSemigroup[A] with Monoid[Rerunnable[A]] {
      final val empty: Rerunnable[A] = Rerunnable.rerunnableInstance.pure(A.empty)
    }

  final def rerunnableEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], Future[A]](_.run)(futureEq[A](atMost))

  final def rerunnableEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], Future[A]](_.run)(futureEqWithFailure[A](atMost))
}

private[util] trait RerunnableInstances1 {
  final def rerunnableComonad(atMost: Duration): Comonad[Rerunnable] =
    new RerunnableCoflatMap with Comonad[Rerunnable] {
      final def extract[A](x: Rerunnable[A]): A = Await.result(x.run, atMost)
      final def map[A, B](fa: Rerunnable[A])(f: A => B): Rerunnable[B] = fa.map(f)
    }

  implicit def rerunnableSemigroup[A](implicit A: Semigroup[A]): Semigroup[Rerunnable[A]] =
    new RerunnableSemigroup[A]
}

private[util] sealed trait RerunnableCoflatMap extends CoflatMap[Rerunnable] {
  final def coflatMap[A, B](fa: Rerunnable[A])(f: Rerunnable[A] => B): Rerunnable[B] =
    new Rerunnable[B] {
      final def run: Future[B] = Future(f(fa))
    }
}

private[util] class RerunnableMonadError extends MonadError[Rerunnable, Throwable] {
  final def pure[A](a: A): Rerunnable[A] = Rerunnable.const(a)

  override final def map[A, B](fa: Rerunnable[A])(f: A => B): Rerunnable[B] = fa.map(f)
  override final def product[A, B](fa: Rerunnable[A], fb: Rerunnable[B]): Rerunnable[(A, B)] = fa.product(fb)

  final def flatMap[A, B](fa: Rerunnable[A])(f: A => Rerunnable[B]): Rerunnable[B] = fa.flatMap(f)

  final def raiseError[A](e: Throwable): Rerunnable[A] = Rerunnable.raiseError(e)

  final def handleErrorWith[A](fa: Rerunnable[A])(f: Throwable => Rerunnable[A]): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = fa.run.rescue {
      case error => f(error).run
    }
  }

  override final def attempt[A](fa: Rerunnable[A]): Rerunnable[Either[Throwable, A]] = fa.liftToTry.map {
    case Return(a)  => Right[Throwable, A](a)
    case Throw(err) => Left[Throwable, A](err)
  }

  final def tailRecM[A, B](a: A)(f: A => Rerunnable[Either[A, B]]): Rerunnable[B] = f(a).flatMap {
    case Right(b)    => pure(b)
    case Left(nextA) => tailRecM(nextA)(f)
  }
}

private[util] sealed class RerunnableSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Rerunnable[A]] {
  final def combine(fx: Rerunnable[A], fy: Rerunnable[A]): Rerunnable[A] = fx.product(fy).map {
    case (x, y) => A.combine(x, y)
  }
}
