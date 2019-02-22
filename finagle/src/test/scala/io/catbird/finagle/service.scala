package io.catbird.finagle

import cats.instances.int._
import cats.kernel.Eq
import cats.laws.discipline._
import cats.laws.discipline.eq._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Service
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class ServiceSuite extends FunSuite with Discipline with
  ServiceInstances with ArbitraryInstances with EqInstances {
  implicit val eq: Eq[Service[Int, Int]] = serviceEq(1.second)

  checkAll("Service", CategoryTests[Service].compose[Int, Int, Int, Int])
  checkAll("Service", CategoryTests[Service].category[Int, Int, Int, Int])
  checkAll("Service", ProfunctorTests[Service].profunctor[Int, Int, Int, Int, Int, Int])
}
