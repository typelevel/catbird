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
    val twitterTimer: util.Timer = new util.JavaTimer()
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
