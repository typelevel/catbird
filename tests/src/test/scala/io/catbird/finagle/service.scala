package io.catbird.finagle

import cats.std.int._
import cats.Eq
import cats.data.Kleisli
import cats.laws.discipline._
import cats.laws.discipline.eq._
import com.twitter.bijection.InversionFailure
import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.util.Future
import io.catbird.tests.finagle.{ ArbitraryInstances, EqInstances }
import io.catbird.util._
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import scala.util.{ Failure, Success }

class ServiceSuite extends FunSuite with Discipline with
  ServiceInstances with ServiceConversions with ArbitraryInstances with EqInstances {
  implicit val eq: Eq[Service[Int, Int]] = serviceEq(1.second)

  checkAll("Service", CategoryTests[Service].compose[Int, Int, Int, Int])
  checkAll("Service", CategoryTests[Service].category[Int, Int, Int, Int])
  checkAll("Service", ProfunctorTests[Service].profunctor[Int, Int, Int, Int, Int, Int])

  test("Service[Int, String] should round-trip through Kleisli[Future, Int, String]") {
    val service = new Service[Int, String] {
      def apply(i: Int) = Future.value(i.toString)
    }

    assert(Success(service) === serviceToKleisli.invert(serviceToKleisli(service)))
  }

  test("Non-Service Kleisli[Future, Int, String] should fail with an InversionFailure") {
    val kleisli: Kleisli[Future, Int, String] = Kleisli.kleisliArrow[Future].lift(_.toString)

    assert(
      serviceToKleisli.invert(kleisli) match {
        case Failure(InversionFailure(_, _)) => true
        case _ => false
      }
    )
  }
}
