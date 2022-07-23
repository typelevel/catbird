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

import cats.effect.laws.discipline.EffectTests
import cats.effect.laws.discipline.arbitrary.catsEffectLawsArbitraryForIO
import cats.effect.laws.util.{ TestContext, TestInstances }
import cats.effect.{ Bracket, IO }
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.Eq
import cats.laws.discipline.arbitrary._
import com.twitter.util.{ Await, Monitor, Throw }
import org.typelevel.catbird.util.{ ArbitraryInstances, Rerunnable }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class RerunnableSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with TestInstances {
  implicit val context: TestContext = TestContext()
  implicit def rerunnableEq[A](implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], IO[A]](rerunnableToIO)

  checkAll("Rerunnable[Int]", EffectTests[Rerunnable].effect[Int, Int, Int])

  test("Exceptions thrown by release are handled by Monitor") {
    val useException = new Exception("thrown by use")
    val releaseException = new Exception("thrown by release")

    var monitoredException: Throwable = null
    val monitor = Monitor.mk { case e => monitoredException = e; true; }

    val rerunnable = Bracket[Rerunnable, Throwable]
      .bracket(Rerunnable.Unit)(_ => Rerunnable.raiseError(useException))(_ => Rerunnable.raiseError(releaseException))
      .liftToTry

    val result = Await.result(Monitor.using(monitor)(rerunnable.run))

    assert(result == Throw(useException))
    assert(monitoredException == releaseException)
  }
}
