package io.catbird.finagle

import cats.instances.int._
import cats.kernel.Eq
import cats.laws.discipline._
import cats.laws.discipline.eq._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Service
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class ServiceSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ServiceInstances
    with ArbitraryInstances
    with EqInstances {
  implicit val eq: Eq[Service[Boolean, Int]] = serviceEq(1.second)

  checkAll("Service", CategoryTests[Service].compose[Boolean, Int, Boolean, Int])
  checkAll("Service", CategoryTests[Service].category[Boolean, Int, Boolean, Int])
  checkAll("Service", ProfunctorTests[Service].profunctor[Boolean, Int, Boolean, Int, Boolean, Int])
}
