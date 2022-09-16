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

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect.Clock
import com.twitter.util.Await
import org.typelevel.catbird.util.Rerunnable
import org.scalatest.Outcome
import org.scalatest.concurrent.Eventually
import org.scalatest.funsuite.FixtureAnyFunSuite

/**
 * We'll use `eventually` and a reasonably big tolerance here to prevent CI from failing if it is a bit slow.
 *
 * Technically the implementation is just an extremely thin wrapper around `System.currentTimeMillis()` and
 * `System.nanoTime()` so as long as the result is the same order of magnitude (and therefore the unit-conversion is
 * correct) we should be fine.
 */
class RerunnableClockSuite extends FixtureAnyFunSuite with Eventually {

  protected final class FixtureParam {
    def now: Instant = Instant.now()

    val clock: Clock[Rerunnable] = RerunnableClock()
  }

  test("Retrieval of real time") { f =>
    eventually {
      val result = Await.result(
        f.clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).run
      )

      assert(java.time.Duration.between(result, f.now).abs().toMillis < 50)
    }
  }

  test("Retrieval of monotonic time") { f =>
    eventually {
      val result = Await.result(
        f.clock.monotonic(TimeUnit.NANOSECONDS).run
      )

      val durationBetween = Math.abs(System.nanoTime() - result)
      assert(TimeUnit.MILLISECONDS.convert(durationBetween, TimeUnit.NANOSECONDS) < 5)
    }
  }

  override protected def withFixture(test: OneArgTest): Outcome = withFixture(test.toNoArgTest(new FixtureParam))
}
