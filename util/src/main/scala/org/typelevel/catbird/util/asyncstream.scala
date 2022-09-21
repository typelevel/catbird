/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.catbird
package util

import cats.{ Alternative, Eq, Monad, Monoid, Semigroup, StackSafeMonad }
import com.twitter.concurrent._
import com.twitter.util._

trait AsyncStreamInstances extends AsyncStreamInstances1 {
  implicit final val asyncStreamInstances: Monad[AsyncStream] with Alternative[AsyncStream] =
    new StackSafeMonad[AsyncStream] with Alternative[AsyncStream] {
      final def pure[A](a: A): AsyncStream[A] = AsyncStream.of(a)
      final def flatMap[A, B](fa: AsyncStream[A])(f: A => AsyncStream[B]): AsyncStream[B] = fa.flatMap(f)
      override final def map[A, B](fa: AsyncStream[A])(f: A => B): AsyncStream[B] = fa.map(f)

      def empty[A]: AsyncStream[A] = AsyncStream.empty[A]
      def combineK[A](x: AsyncStream[A], y: AsyncStream[A]): AsyncStream[A] = x ++ y
    }

  implicit final def asyncStreamSemigroup[A](implicit A: Semigroup[A]): Semigroup[AsyncStream[A]] =
    new AsyncStreamSemigroup[A]

  final def asyncStreamEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[AsyncStream[A]] = new Eq[AsyncStream[A]] {
    final def eqv(x: AsyncStream[A], y: AsyncStream[A]): scala.Boolean = Await.result(
      x.head.join(y.head).map((Eq[Option[A]].eqv _).tupled),
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
