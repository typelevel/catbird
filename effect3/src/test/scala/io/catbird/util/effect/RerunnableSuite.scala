package io.catbird.util.effect

import cats.effect.MonadCancel
import cats.effect.kernel.testkit.SyncTypeGenerators
import cats.effect.laws.{ ClockTests, MonadCancelTests, SyncTests }
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.laws.discipline.MonadErrorTests
import cats.laws.discipline.arbitrary._
import com.twitter.util.{ Await, Monitor, Throw }
import io.catbird.util.{ ArbitraryInstances, EqInstances, Rerunnable }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class RerunnableSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with SyncTypeGenerators
    with EqInstances
    with Runners {

  checkAll("Rerunnable[Int]", ClockTests[Rerunnable].clock)
  checkAll("Rerunnable[Int]", MonadErrorTests[Rerunnable, Throwable].monadError[Int, Int, Int])
  checkAll("Rerunnable[Int]", MonadCancelTests[Rerunnable, Throwable].monadCancel[Int, Int, Int])
  checkAll("Rerunnable[Int]", SyncTests[Rerunnable].sync[Int, Int, Int])

  test("Exceptions thrown by release are handled by Monitor") {
    val useException = new Exception("thrown by use")
    val releaseException = new Exception("thrown by release")

    var monitoredException: Throwable = null
    val monitor = Monitor.mk { case e => monitoredException = e; true; }

    val rerunnable = MonadCancel[Rerunnable, Throwable]
      .bracket(Rerunnable.Unit)(_ => Rerunnable.raiseError(useException))(_ => Rerunnable.raiseError(releaseException))
      .liftToTry

    val result = Await.result(Monitor.using(monitor)(rerunnable.run))

    assert(result == Throw(useException))
    assert(monitoredException == releaseException)
  }
}
