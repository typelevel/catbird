package io.catbird.finagle

import com.twitter.finagle.Service
import com.twitter.util.Future
import org.scalatest.FunSuite
import scala.util.Success

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
}
