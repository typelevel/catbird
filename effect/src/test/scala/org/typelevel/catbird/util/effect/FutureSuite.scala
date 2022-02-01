package org.typelevel.catbird.util.effect

import cats.Eq
import cats.effect.laws.discipline.BracketTests
import cats.effect.laws.util.{ TestContext, TestInstances }
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.DurationOps._
import com.twitter.util.Future
import org.typelevel.catbird.util.{ ArbitraryInstances, futureEqWithFailure }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class FutureSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with TestInstances {
  implicit val context: TestContext = TestContext()
  implicit def futureEq[A](implicit A: Eq[A]): Eq[Future[A]] =
    futureEqWithFailure(1.seconds)

  checkAll("Future[Int]", BracketTests[Future, Throwable].bracket[Int, Int, Int])
}
