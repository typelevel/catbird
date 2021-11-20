package io.catbird.util
package effect

import com.twitter.util.Monitor
import org.scalatest.funsuite.AnyFunSuite
import org.scalacheck.{ Arbitrary, Prop, Test }
import org.scalacheck.rng.Seed
import org.scalacheck.util.Pretty
import org.scalatest.prop.Configuration
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

abstract class BaseLawSuite extends AnyFunSuite with FunSuiteDiscipline with Configuration with EqInstances {
  protected val monitor = Monitor.mk { case e => println("Monitored: " + e); true }

  override protected def withFixture(test: NoArgTest) =
    Monitor.using(monitor)(test())

  // For debugging an individual failing law.  Call this and use `testOnly <suite> -- -z withSeed`.  Eg:
  //
  // cats law:
  //   import cats.kernel.laws.discipline._
  //   testLawWithSeed(
  //     cats.laws.ApplicativeLaws[Rerunnable].applicativeIdentity[Int],
  //     "NRzb_Wsi6ki82wDgbifBkUvntAPN5kaO8FbKSYpKXiF=",
  //   )
  //
  // cats-effect law:
  //   testLawWithSeed(
  //     cats.effect.laws.MonadCancelLaws[Rerunnable, Throwable].uncancelablePollInverseNestIsUncancelable[Int],
  //     "957izWLFn5kHIug2eCpFHBfajl4zwQGrNdVvC5XWtFK=",
  //   )
  //
  // You may need to wrangle the law into a Function1 with .tupled.
  protected def testLawWithSeed[A: Arbitrary: * => Pretty, L: * => Prop](law: A => L, seed: String) =
    test("withSeed") {
      Checkers.check(Prop.forAll(law), Test.Parameters.default.withInitialSeed(Seed.fromBase64(seed).get))
    }
}
