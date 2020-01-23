package io.catbird.util.effect

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
import io.catbird.util.{ ArbitraryInstances, Rerunnable }
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
