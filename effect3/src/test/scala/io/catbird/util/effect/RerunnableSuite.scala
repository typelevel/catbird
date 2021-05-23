package io.catbird.util.effect

import cats.effect.MonadCancel
import cats.effect.kernel.testkit.SyncTypeGenerators
import cats.effect.laws.SyncTests
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.laws.discipline.arbitrary._
import com.twitter.util.{ Await, Monitor, Throw }
import io.catbird.util.Rerunnable

class RerunnableSuite extends BaseLawSuite with SyncTypeGenerators with Runners {

  // This includes tests for Clock, MonadCancel, and MonadError
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
