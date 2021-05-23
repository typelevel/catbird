package io.catbird.util
package effect

import com.twitter.util.Monitor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

abstract class BaseLawSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with EqInstances {
  protected val monitor = Monitor.mk { case e => println("Monitored: " + e); true }

  override protected def withFixture(test: NoArgTest) =
    Monitor.using(monitor)(test())
}
