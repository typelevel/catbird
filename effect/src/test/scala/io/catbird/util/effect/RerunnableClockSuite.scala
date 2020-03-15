package io.catbird.util.effect

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect.Clock
import com.twitter.util.Await
import io.catbird.util.Rerunnable
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite

class RerunnableClockSuite extends FixtureAnyFunSuite {

  protected final class FixtureParam {
    def now: Instant = Instant.now()

    val clock: Clock[Rerunnable] = RerunnableClock()
  }

  test("Retrieval of real time") { f =>
    val result = Await.result(
      f.clock.realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli).run
    )

    assert(java.time.Duration.between(result, f.now).abs().toMillis < 50)
  }

  test("Retrieval of monotonic time") { f =>
    val result = Await.result(
      f.clock.monotonic(TimeUnit.NANOSECONDS).run
    )

    val durationBetween = Math.abs(System.nanoTime() - result)
    assert(TimeUnit.MILLISECONDS.convert(durationBetween, TimeUnit.NANOSECONDS) < 5)
  }

  override protected def withFixture(test: OneArgTest): Outcome = withFixture(test.toNoArgTest(new FixtureParam))
}
