package io.catbird
package util

import cats.{ CoflatMap, Eq, Monad, Semigroup }
import com.twitter.concurrent._
import com.twitter.util._
import scala.util.{ Either, Right, Left }

trait AsyncStreamInstances extends AsyncStreamInstances1 {

  implicit final val asyncStreamInstances: Monad[AsyncStream] with CoflatMap[AsyncStream] =
    new AsyncStreamCoflatMap with Monad[AsyncStream] {
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

}

private[util] abstract class AsyncStreamCoflatMap extends CoflatMap[AsyncStream] {
  final def coflatMap[A, B](fa: AsyncStream[A])(f: AsyncStream[A] => B): AsyncStream[B] = AsyncStream(f(fa))

  /**
    * Note that this implementation is not stack-safe.
    */
  final def tailRecM[A, B](a: A)(f: A => AsyncStream[Either[A,B]]): AsyncStream[B] = f(a).flatMap {
    case Left(a1) => tailRecM(a1)(f)
    case Right(b) => AsyncStream.of(b)
  }
}

private[util] class AsyncStreamSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[AsyncStream[A]] {
  final def combine(fa: AsyncStream[A], fb: AsyncStream[A]): AsyncStream[A] = fa.flatMap { a =>
    fb.map( b => A.combine(a,b) )
  }

}
