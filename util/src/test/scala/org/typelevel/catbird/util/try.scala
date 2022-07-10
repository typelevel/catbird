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

package org.typelevel.catbird.util

import cats.instances.either._
import cats.instances.int._
import cats.instances.option._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.discipline.{ MonoidTests, SemigroupTests }
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.{ MonadErrorTests, TraverseTests }
import com.twitter.util.{ Return, Throw, Try }
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

trait TryTest extends ArbitraryInstances with EqInstances with TryInstances

class TrySuite extends CatbirdSuite with TryTest {
  checkAll("Try[Int]", MonadErrorTests[Try, Throwable].monadError[Int, Int, Int])
  checkAll("Try[Int]", TraverseTests[Try].traverse[Int, Int, Int, Int, Option, Option])
  checkAll("Try[Int]", SemigroupTests[Try[Int]](twitterTrySemigroup[Int]).semigroup)
  checkAll("Try[Int]", MonoidTests[Try[Int]].monoid)
}

class TrySpec extends AnyPropSpec with ScalaCheckPropertyChecks with TryTest {
  property("Equality for Try should use universal equality for Throw") {
    case class SomeError(message: String) extends Exception(message)

    forAll((s: String) => assert(twitterTryEq[Int].eqv(Throw(SomeError(s)), Throw(SomeError(s)))))
  }

  property("Equality for Try should never mix up Return and Throw") {
    forAll((i: Int) => assert(!twitterTryEq[Int].eqv(Throw(new Exception), Return(i))))
  }
}
