package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, MonadError, MonadRec, Monoid, Semigroup }
import cats.data.Xor
import com.twitter.util.{ Await, Duration, Future, FuturePool, Try }
import java.lang.Throwable
import scala.Unit
import scala.annotation.tailrec

abstract class Rerunnable[A] { self =>
  def run: Future[A]

  final def map[B](f: A => B): Rerunnable[B] = new Rerunnable[B] {
    final def run: Future[B] = self.run.map(f)
  }

  final def flatMap[B](f: A => Rerunnable[B]): Rerunnable[B] = new Rerunnable.Bind[A, B](this, f)

  final def flatMapF[B](f: A => Future[B]): Rerunnable[B] = new Rerunnable[B] {
    final def run: Future[B] = self.run.flatMap(f)
  }

  final def product[B](other: Rerunnable[B]): Rerunnable[(A, B)] = new Rerunnable[(A, B)] {
    final def run: Future[(A, B)] = self.run.join(other.run)
  }

  final def liftToTry: Rerunnable[Try[A]] = new Rerunnable[Try[A]] {
    final def run: Future[Try[A]] = self.run.liftToTry
  }

  @tailrec
  final def step: Rerunnable[A] = this match {
    case outer: Rerunnable.Bind[_, A] => outer.fa match {
      case inner: Rerunnable.Bind[_, _] => inner.fa.flatMap(x => inner.ff(x).flatMap(outer.ff)).step
      case _ => this
    }
    case _ => this
  }
}

final object Rerunnable extends RerunnableInstances1 {
  private[util] class Bind[A, B](val fa: Rerunnable[A], val ff: A => Rerunnable[B]) extends Rerunnable[B] {
    final def run: Future[B] = step match {
      case bind: Bind[A, B] => bind.fa.run.flatMap(a => bind.ff(a).run)
      case other => other.run
    }
  }

  def apply[A](a: => A): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = Future(a)
  }

  def fromFuture[A](fa: => Future[A]): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = fa
  }

  def withFuturePool[A](pool: FuturePool)(a: => A): Rerunnable[A] = new Rerunnable[A] {
    final def run: Future[A] = pool(a)
  }

  val Unit: Rerunnable[Unit] = new Rerunnable[Unit] {
    final def run: Future[Unit] = Future.Unit
  }

  implicit val rerunnableInstance: MonadError[Rerunnable, Throwable] with CoflatMap[Rerunnable]
      with MonadRec[Rerunnable] =
    new RerunnableCoflatMap with MonadError[Rerunnable, Throwable] with MonadRec[Rerunnable] {
      final def pure[A](a: A): Rerunnable[A] = new Rerunnable[A] {
        final def run: Future[A] = Future.value(a)
      }

      override final def map[A, B](fa: Rerunnable[A])(f: A => B): Rerunnable[B] = fa.map(f)

      override final def product[A, B](fa: Rerunnable[A], fb: Rerunnable[B]): Rerunnable[(A, B)] =
        fa.product(fb)

      final def flatMap[A, B](fa: Rerunnable[A])(f: A => Rerunnable[B]): Rerunnable[B] =
        fa.flatMap(f)

      final def raiseError[A](e: Throwable): Rerunnable[A] = new Rerunnable[A] {
        final def run: Future[A] = Future.exception[A](e)
      }

      final def handleErrorWith[A](fa: Rerunnable[A])(
        f: Throwable => Rerunnable[A]
      ): Rerunnable[A] = new Rerunnable[A] {
        final def run: Future[A] = fa.run.rescue {
          case error => f(error).run
        }
      }

      final def tailRecM[A, B](a: A)(f: A => Rerunnable[Xor[A, B]]): Rerunnable[B] =
        f(a).flatMap {
          case Xor.Left(a1) => tailRecM(a1)(f)
          case Xor.Right(b) => pure(b)
        }
    }

  implicit final def rerunnableMonoid[A](implicit A: Monoid[A]): Monoid[Rerunnable[A]] =
    new RerunnableSemigroup[A] with Monoid[Rerunnable[A]] {
      final def empty: Rerunnable[A] = Rerunnable.rerunnableInstance.pure(A.empty)
    }

  final def rerunnableEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Rerunnable[A]] =
    futureEq[A](atMost).on(_.run)

  final def rerunnableEqWithFailure[A](atMost: Duration)(implicit A: Eq[A]): Eq[Rerunnable[A]] =
    futureEqWithFailure[A](atMost).on(_.run)
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

private[util] sealed abstract class RerunnableCoflatMap extends CoflatMap[Rerunnable] {
  final def coflatMap[A, B](fa: Rerunnable[A])(f: Rerunnable[A] => B): Rerunnable[B] =
    new Rerunnable[B] {
      final def run: Future[B] = Future(f(fa))
    }
}

private[util] sealed class RerunnableSemigroup[A](implicit A: Semigroup[A])
  extends Semigroup[Rerunnable[A]] {
    final def combine(fx: Rerunnable[A], fy: Rerunnable[A]): Rerunnable[A] = fx.product(fy).map {
      case (x, y) => A.combine(x, y)
    }
  }
