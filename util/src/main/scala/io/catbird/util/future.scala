package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, Eval, MonadError, Monoid, Semigroup }
import com.twitter.util.{ Await, Duration, Future }

trait FutureInstances extends FutureInstances1 {
  implicit final val futureInstance: MonadError[Future, Throwable] with CoflatMap[Future] =
    new FutureCoflatMap with MonadError[Future, Throwable] {
      final def pure[A](x: A): Future[A] = Future.value(x)
      override final def pureEval[A](x: Eval[A]): Future[A] = Future(x.value)
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
    }

  implicit def futureSemigroup[A](implicit A: Semigroup[A]): Semigroup[Future[A]] =
    new FutureSemigroup[A]

  def futureEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Future[A]] = new Eq[Future[A]] {
    def eqv(x: Future[A], y: Future[A]): Boolean = Await.result(
      (x.join(y)).map {
        case (xa, ya) => A.eqv(xa, ya)
      },
      atMost
    )
  }
}

private[util] trait FutureInstances1 {
  def futureComonad(atMost: Duration): Comonad[Future] =
    new FutureCoflatMap with Comonad[Future] {
      def extract[A](x: Future[A]): A = Await.result(x, atMost)
      def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
    }

  implicit def futureMonoid[A](implicit A: Monoid[A]): Monoid[Future[A]] =
    new FutureSemigroup[A] with Monoid[Future[A]] {
      def empty: Future[A] = Future.value(A.empty)
    }
}

private[util] abstract class FutureCoflatMap extends CoflatMap[Future] {
  def coflatMap[A, B](fa: Future[A])(f: Future[A] => B): Future[B] = Future(f(fa))
}

private[util] class FutureSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Future[A]] {
  def combine(fx: Future[A], fy: Future[A]): Future[A] = (fx join fy).map((A.combine _).tupled)
}
