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

import cats.effect.Timer
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite
import com.twitter.util
import com.twitter.util.{ Await, Future }
import org.typelevel.catbird.util.Rerunnable

import scala.concurrent.duration._

class RerunnableTimerSuite extends FixtureAnyFunSuite {

  protected final class FixtureParam {
    val twitterTimer: util.Timer = new util.JavaTimer
  }

  test("A timer can be used to delay execution") { f =>
    implicit val timer: Timer[Rerunnable] = RerunnableTimer(f.twitterTimer)

    val result = Await.result(
      Future.selectIndex(
        Vector(
          for {
            _ <- Timer[Rerunnable].sleep(100.milliseconds).run
            r <- Future.value("slow")
          } yield r,
          Future.value("fast").delayed(util.Duration.fromMilliseconds(50))(f.twitterTimer)
        )
      )
    )

    assert(result == 1) // The first future is delayed for longer, so we expect the second one to win
  }

  override protected def withFixture(test: OneArgTest): Outcome = withFixture(test.toNoArgTest(new FixtureParam))
}
