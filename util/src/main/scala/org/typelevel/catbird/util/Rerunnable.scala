package org.typelevel.catbird.util

import cats.{
  Applicative,
  CoflatMap,
  CommutativeApplicative,
  Comonad,
  Eq,
  Monad,
  MonadError,
  Monoid,
  Parallel,
  Semigroup,
  ~>
}
import com.twitter.util.{ Await, Duration, Future, FuturePool, Return, Throw, Try }
import java.lang.Throwable

import scala.{ Boolean, Unit }
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
  private[this] def reassociate[B](bind: Bind[B]): Bind[B] =
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

  implicit final val rerunnableParallelInstance: Parallel.Aux[Rerunnable, Rerunnable.Par] =
    new Parallel[Rerunnable] {
      type F[x] = Rerunnable.Par[x]

      final override val applicative: Applicative[Rerunnable.Par] =
        rerunnableParCommutativeApplicative

      final override val monad: Monad[Rerunnable] =
        rerunnableInstance

      final override val sequential: Rerunnable.Par ~> Rerunnable =
        λ[Rerunnable.Par ~> Rerunnable](Rerunnable.Par.unwrap(_))

      final override val parallel: Rerunnable ~> Rerunnable.Par =
        λ[Rerunnable ~> Rerunnable.Par](Rerunnable.Par(_))
    }

  /**
   * Obtain a [[cats.Eq]] instance for [[Rerunnable]].
   *
   * This version is only useful for successful actions: if one fails, the resulting exception will be thrown.
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def rerunnableEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], Future[A]](_.run)(futureEq[A](atMost))

  /**
   * Obtain a [[cats.Eq]] instance for [[Rerunnable]].
   *
   * This version can also compare failed actions and thus requires an `Eq[Throwable]` in scope.
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def rerunnableEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], Future[A]](_.run)(futureEqWithFailure[A](atMost))

  /**
   * FunctionK instance to convert to Future.
   *
   * Useful when you need a parameter for a `mapK` method, for example you could use
   * [[https://typelevel.org/cats-tagless/ cats-tagless]] in a codebase that mixes [[Rerunnable]] and
   * [[Future]].
   */
  final val toFuture: Rerunnable ~> Future = λ[Rerunnable ~> Future](_.run)
}

private[util] trait RerunnableInstances1 extends RerunnableParallelNewtype {

  /**
   * Obtain a [[cats.Comonad]] instance for [[Rerunnable]]
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def rerunnableComonad(atMost: Duration): Comonad[Rerunnable] =
    new RerunnableCoflatMap with Comonad[Rerunnable] {
      final def extract[A](x: Rerunnable[A]): A = Await.result(x.run, atMost)
      final def map[A, B](fa: Rerunnable[A])(f: A => B): Rerunnable[B] = fa.map(f)
    }

  implicit def rerunnableSemigroup[A](implicit A: Semigroup[A]): Semigroup[Rerunnable[A]] =
    new RerunnableSemigroup[A]
}

private[util] trait RerunnableParallelNewtype {
  type Par[+A] = Par.Type[A]

  object Par {
    type Type[+A] = Rerunnable[A]

    def apply[A](fa: Rerunnable[A]): Type[A] =
      fa.asInstanceOf[Type[A]]

    def unwrap[A](fa: Type[A]): Rerunnable[A] =
      fa.asInstanceOf[Rerunnable[A]]
  }

  implicit final val rerunnableParCommutativeApplicative: CommutativeApplicative[Par] =
    new CommutativeApplicative[Par] {
      import Par.unwrap

      final override def pure[A](x: A): Rerunnable.Par[A] =
        Par(Rerunnable.const(x))
      final override def map2[A, B, Z](
        fa: Rerunnable.Par[A],
        fb: Rerunnable.Par[B]
      )(f: (A, B) => Z): Rerunnable.Par[Z] =
        // Future.join runs futures in parallel
        Par(Rerunnable.fromFuture(Future.join(unwrap(fa).run, unwrap(fb).run)).map(f.tupled))
      final override def ap[A, B](ff: Rerunnable.Par[A => B])(fa: Rerunnable.Par[A]): Rerunnable.Par[B] =
        map2(ff, fa)(_(_))
      final override def product[A, B](fa: Rerunnable.Par[A], fb: Rerunnable.Par[B]): Rerunnable.Par[(A, B)] =
        map2(fa, fb)((_, _))
      final override def map[A, B](fa: Rerunnable.Par[A])(f: A => B): Rerunnable.Par[B] =
        Par(unwrap(fa).map(f))
      final override def unit: Rerunnable.Par[scala.Unit] =
        Par(Rerunnable.Unit)
    }

  final def rerunnableParEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Par[A]] = new Eq[Par[A]] {
    import Par.unwrap

    final override def eqv(x: Par[A], y: Par[A]): Boolean =
      Rerunnable.rerunnableEq(atMost).eqv(unwrap(x), unwrap(y))
  }

  final def rerunnableParEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Par[A]] =
    new Eq[Par[A]] {
      import Par.unwrap

      override def eqv(x: Par[A], y: Par[A]): Boolean =
        Rerunnable.rerunnableEqWithFailure(atMost)(A, T).eqv(unwrap(x), unwrap(y))
    }
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
    final def run: Future[A] = fa.run.rescue { case error =>
      f(error).run
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
  final def combine(fx: Rerunnable[A], fy: Rerunnable[A]): Rerunnable[A] = fx.product(fy).map { case (x, y) =>
    A.combine(x, y)
  }
}
