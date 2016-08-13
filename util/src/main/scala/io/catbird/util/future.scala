package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, MonadError, Monoid, Semigroup }
import cats.data.Xor
import com.twitter.util.{ Await, Duration, Future, Try }
import java.lang.Throwable
import scala.Boolean

trait FutureInstances extends FutureInstances1 {
  implicit final val twitterFutureInstance: MonadError[Future, Throwable] with CoflatMap[Future] =
    new FutureCoflatMap with MonadError[Future, Throwable] {
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

      final def tailRecM[A, B](a: A)(f: A => Future[Xor[A, B]]): Future[B] =
        f(a).flatMap {
          case Xor.Left(a1) => tailRecM(a1)(f)
          case Xor.Right(b) => Future.value(b)
        }
    }

  implicit final def twitterFutureSemigroup[A](implicit A: Semigroup[A]): Semigroup[Future[A]] =
    new FutureSemigroup[A]

  final def futureEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Future[A]] = new Eq[Future[A]] {
    final def eqv(x: Future[A], y: Future[A]): Boolean = Await.result(
      x.join(y).map {
        case (xa, ya) => A.eqv(xa, ya)
      },
      atMost
    )
  }

  final def futureEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Future[A]] =
    futureEq[Try[A]](atMost).on(_.liftToTry)
}

private[util] trait FutureInstances1 {
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

private[util] sealed abstract class FutureCoflatMap extends CoflatMap[Future] {
  final def coflatMap[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future(f(fa))
}

private[util] sealed class FutureSemigroup[A](implicit A: Semigroup[A])
  extends Semigroup[Future[A]] {
    final def combine(fx: Future[A], fy: Future[A]): Future[A] = fx.join(fy).map {
      case (x, y) => A.combine(x, y)
    }
  }
