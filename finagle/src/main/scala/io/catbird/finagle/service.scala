package io.catbird.finagle

import cats.arrow.Category
import cats.data.Kleisli
import cats.functor.Profunctor
import io.catbird.util.futureInstance
import com.twitter.bijection.{ Injection, InversionFailure }
import com.twitter.finagle.Service
import com.twitter.util.{ Future }
import scala.util.Success

trait ServiceInstances {
  implicit val serviceInstance: Category[Service] with Profunctor[Service] =
    new Category[Service] with Profunctor[Service] {
      def id[A]: Service[A, A] = Service.mk(futureInstance.pure)

      def compose[A, B, C](f: Service[B, C], g: Service[A, B]): Service[A, C] =
        Service.mk(a => g(a).flatMap(f))

      def dimap[A, B, C, D](fab: Service[A, B])(f: C => A)(g: B => D): Service[C, D] =
        Service.mk(c => fab.map(f)(c).map(g))

      override def lmap[A, B, C](fab: Service[A, B])(f: C => A): Service[C, B] = fab.map(f)
    }
}

trait ServiceConversions {
  implicit def serviceToKleisli[I, O]: Injection[Service[I, O], Kleisli[Future, I, O]] =
    Injection.build[Service[I, O], Kleisli[Future, I, O]](Kleisli.function) {
      case Kleisli(service: Service[I, O]) => Success(service)
      case other => InversionFailure.failedAttempt(other)
    }
}
