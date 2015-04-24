package io.catbird.finagle

import cats.data.Kleisli
import com.twitter.bijection.{ Injection, InversionFailure }
import com.twitter.finagle.Service
import com.twitter.util.{ Future }
import scala.util.Success

trait ServiceConversions {
  implicit def serviceToKleisli[I, O]: Injection[Service[I, O], Kleisli[Future, I, O]] =
    Injection.build[Service[I, O], Kleisli[Future, I, O]](Kleisli.kleisli) {
      case Kleisli(service: Service[I, O]) => Success(service)
      case other => InversionFailure.failedAttempt(other)
    }
}
