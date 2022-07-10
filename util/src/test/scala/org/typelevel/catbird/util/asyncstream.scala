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

import cats.Eq
import cats.instances.int._
import cats.instances.tuple._
import cats.kernel.laws.discipline.{ MonoidTests, SemigroupTests }
import cats.laws.discipline._
import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.DurationOps._

class AsyncStreamSuite extends CatbirdSuite with AsyncStreamInstances with ArbitraryInstances {
  implicit val eqAsyncStreamInt: Eq[AsyncStream[Int]] = asyncStreamEq(1.second)
  implicit val eqAsyncStreamAsyncStreamInt: Eq[AsyncStream[AsyncStream[Int]]] = asyncStreamEq(1.second)
  implicit val eqAsyncStreamIntIntInt: Eq[AsyncStream[(Int, Int, Int)]] = asyncStreamEq[(Int, Int, Int)](1.second)

  checkAll("AsyncStream[Int]", AlternativeTests[AsyncStream].alternative[Int, Int, Int])
  checkAll("AsyncStream[Int]", MonadTests[AsyncStream].monad[Int, Int, Int])
  checkAll("AsyncStream[Int]", SemigroupTests[AsyncStream[Int]](asyncStreamSemigroup[Int]).semigroup)
  checkAll("AsyncStream[Int]", MonoidTests[AsyncStream[Int]].monoid)
}
