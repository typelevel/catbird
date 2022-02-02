package org.typelevel.catbird.util.effect

import cats.Eq
import cats.effect.laws.MonadCancelTests
import cats.instances.all._
import cats.laws.discipline.MonadErrorTests
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.DurationOps._
import com.twitter.util.Future
import org.typelevel.catbird.util.{ ArbitraryInstances, EqInstances, futureEqWithFailure }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class FutureSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ArbitraryInstances
    with EqInstances {

  implicit def futureEq[A](implicit A: Eq[A]): Eq[Future[A]] =
    futureEqWithFailure(1.seconds)

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", MonadCancelTests[Future, Throwable].monadCancel[Int, Int, Int])
}
