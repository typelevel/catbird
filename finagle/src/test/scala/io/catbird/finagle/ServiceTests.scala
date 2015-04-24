package io.catbird.finagle

import cats.data.Kleisli
import com.twitter.bijection.InversionFailure
import com.twitter.finagle.Service
import com.twitter.util.Future
import io.catbird.util._
import org.scalatest.FunSuite
import scala.util.{ Failure, Success }

/**
 * TODO: real tests
 */
class ServiceTests extends FunSuite with ServiceConversions {
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
