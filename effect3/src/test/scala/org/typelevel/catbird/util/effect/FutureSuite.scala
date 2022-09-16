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

package org.typelevel.catbird.util.effect

import cats.Eq
import cats.effect.laws.MonadCancelTests
import cats.instances.all._
import cats.laws.discipline.MonadErrorTests
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.DurationOps._
import com.twitter.util.Future
import org.typelevel.catbird.util.{ ArbitraryInstances, EqInstances, futureEqWithFailure }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class FutureSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with EqInstances {

  implicit def futureEq[A](implicit A: Eq[A]): Eq[Future[A]] =
    futureEqWithFailure(1.seconds)

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", MonadCancelTests[Future, Throwable].monadCancel[Int, Int, Int])
}
