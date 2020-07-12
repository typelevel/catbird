package io.catbird.util

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
import com.twitter.util.{ Await, Duration, Future, Try }
import java.lang.Throwable

import scala.Boolean
import scala.util.{ Either, Left, Right }

trait FutureInstances extends FutureInstances1 {
  implicit final val twitterFutureInstance: MonadError[Future, Throwable] with CoflatMap[Future] =
    new FutureCoflatMap with FutureMonadError

  implicit final def twitterFutureSemigroup[A](implicit A: Semigroup[A]): Semigroup[Future[A]] =
    new FutureSemigroup[A]

  /**
   * Obtain a [[cats.Eq]] instance for [[com.twitter.util.Future]].
   *
   * This version is only useful for successful futures: if one of the futures fails, the resulting exception
   * will be thrown.
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def futureEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Future[A]] = new Eq[Future[A]] {
    final def eqv(x: Future[A], y: Future[A]): Boolean = Await.result(
      x.join(y).map {
        case (xa, ya) => A.eqv(xa, ya)
      },
      atMost
    )
  }

  /**
   * Obtain a [[cats.Eq]] instance for [[com.twitter.util.Future]].
   *
   * This version can also compare failed futures and thus requires an `Eq[Throwable]` in scope.
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def futureEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Future[A]] =
    Eq.by[Future[A], Future[Try[A]]](_.liftToTry)(futureEq[Try[A]](atMost))

  implicit final val twitterFutureParallelInstance: Parallel.Aux[Future, FuturePar] =
    new Parallel[Future] {
      type F[x] = FuturePar[x]

      final override val applicative: Applicative[FuturePar] =
        futureParCommutativeApplicative

      final override val monad: Monad[Future] =
        twitterFutureInstance

      final override val sequential: FuturePar ~> Future = λ[FuturePar ~> Future](FuturePar.unwrap(_))

      final override val parallel: Future ~> FuturePar = λ[Future ~> FuturePar](FuturePar(_))
    }
}

private[util] trait FutureInstances1 extends FutureParallelNewtype {

  /**
   * Obtain a [[cats.Comonad]] instance for [[com.twitter.util.Future]].
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def futureComonad(atMost: Duration): Comonad[Future] =
    new FutureCoflatMap with Comonad[Future] {
      final def extract[A](x: Future[A]): A = Await.result(x, atMost)
      final def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
    }

  implicit final def twitterFutureMonoid[A](implicit A: Monoid[A]): Monoid[Future[A]] =
    new FutureSemigroup[A] with Monoid[Future[A]] {
      final def empty: Future[A] = Future.value(A.empty)
    }
}

private[util] trait FutureParallelNewtype {
  type FuturePar[+A] = FuturePar.Type[A]

  object FuturePar extends internal.Newtype1[Future]

  implicit final val futureParCommutativeApplicative: CommutativeApplicative[FuturePar] =
    new CommutativeApplicative[FuturePar] {
      import FuturePar.{ unwrap, apply => par }

      final override def pure[A](x: A): FuturePar[A] =
        par(Future.value(x))
      final override def map2[A, B, Z](fa: FuturePar[A], fb: FuturePar[B])(f: (A, B) => Z): FuturePar[Z] =
        par(Future.join(unwrap(fa), unwrap(fb)).map(f.tupled)) // Future.join runs futures in parallel
      final override def ap[A, B](ff: FuturePar[A => B])(fa: FuturePar[A]): FuturePar[B] =
        map2(ff, fa)(_(_))
      final override def product[A, B](fa: FuturePar[A], fb: FuturePar[B]): FuturePar[(A, B)] =
        map2(fa, fb)((_, _))
      final override def map[A, B](fa: FuturePar[A])(f: A => B): FuturePar[B] =
        par(unwrap(fa).map(f))
      final override def unit: FuturePar[scala.Unit] =
        par(Future.Unit)
    }

  final def futureParEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[FuturePar[A]] = new Eq[FuturePar[A]] {
    import FuturePar.unwrap

    final override def eqv(x: FuturePar[A], y: FuturePar[A]): Boolean =
      futureEq(atMost).eqv(unwrap(x), unwrap(y))
  }

  final def futureParEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[FuturePar[A]] =
    new Eq[FuturePar[A]] {
      import FuturePar.unwrap

      final override def eqv(x: FuturePar[A], y: FuturePar[A]): Boolean =
        futureEqWithFailure(atMost)(A, T).eqv(unwrap(x), unwrap(y))
    }
}

private[util] trait FutureMonadError extends MonadError[Future, Throwable] {
  final def pure[A](x: A): Future[A] = Future.value(x)
  final def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)
  override final def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
  override final def ap[A, B](f: Future[A => B])(fa: Future[A]): Future[B] = f.join(fa).map {
    case (ab, a) => ab(a)
  }
  override final def product[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] = fa.join(fb)

  final def handleErrorWith[A](fa: Future[A])(f: Throwable => Future[A]): Future[A] =
    fa.rescue {
      case e => f(e)
    }
  final def raiseError[A](e: Throwable): Future[A] = Future.exception(e)

  final def tailRecM[A, B](a: A)(f: A => Future[Either[A, B]]): Future[B] = f(a).flatMap {
    case Right(b)    => pure(b)
    case Left(nextA) => tailRecM(nextA)(f)
  }
}

private[util] sealed abstract class FutureCoflatMap extends CoflatMap[Future] {
  final def coflatMap[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future(f(fa))
}

private[util] sealed class FutureSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Future[A]] {
  final def combine(fx: Future[A], fy: Future[A]): Future[A] = fx.join(fy).map {
    case (x, y) => A.combine(x, y)
  }
}
