package io.catbird
package util

import cats.{ Eq, Monad, Monoid, Semigroup, StackSafeMonad }
import com.twitter.concurrent._
import com.twitter.util._

trait AsyncStreamInstances extends AsyncStreamInstances1 {

  implicit final val asyncStreamInstances: Monad[AsyncStream] =
    new StackSafeMonad[AsyncStream] {
      final def pure[A](a: A): AsyncStream[A] = AsyncStream.of(a)
      final def flatMap[A, B](fa: AsyncStream[A])(f: A => AsyncStream[B]): AsyncStream[B] = fa.flatMap(f)
      override final def map[A, B](fa: AsyncStream[A])(f: A => B): AsyncStream[B] = fa.map(f)
    }

  implicit final def asyncStreamSemigroup[A](implicit A: Semigroup[A]): Semigroup[AsyncStream[A]] =
    new AsyncStreamSemigroup[A]

  final def asyncStreamEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[AsyncStream[A]] = new Eq[AsyncStream[A]] {
    final def eqv(x: AsyncStream[A], y: AsyncStream[A]): scala.Boolean = Await.result(
      x.take(1).toSeq.join(y.take(1).toSeq).map { case (x, y) => x == y },
      atMost
    )
  }

}

trait AsyncStreamInstances1 {

  implicit final def asyncStreamMonoid[A](implicit M: Monoid[A]): Monoid[AsyncStream[A]] =
    new AsyncStreamSemigroup[A] with Monoid[AsyncStream[A]] {
      final def empty: AsyncStream[A] = AsyncStream(M.empty)
    }
}

private[util] class AsyncStreamSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[AsyncStream[A]] {
  final def combine(fa: AsyncStream[A], fb: AsyncStream[A]): AsyncStream[A] = fa.flatMap { a =>
    fb.map(b => A.combine(a, b))
  }

}
