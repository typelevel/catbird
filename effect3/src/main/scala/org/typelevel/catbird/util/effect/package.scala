package org.typelevel.catbird.util

import cats.effect.{ Async, IO }
import com.twitter.util.{ Future, Return, Throw, Try }

import java.lang.Throwable

import cats.effect.Outcome
import cats.effect.kernel.Resource.ExitCase

import scala.util.{ Left, Right }

package object effect extends FutureInstances with RerunnableInstances {

  /**
   * Converts the `Future` to `F`.
   */
  def futureToAsync[F[_], A](fa: => Future[A])(implicit F: Async[F]): F[A] = F.async_ { k =>
    fa.respond {
      case Return(a)  => k(Right(a))
      case Throw(err) => k(Left(err))
    }

    ()
  }

  /**
   * Converts the `Rerunnable` to `IO`.
   */
  final def rerunnableToIO[A](fa: Rerunnable[A]): IO[A] =
    futureToAsync[IO, A](fa.run)

  /**
   * Convert a twitter-util Try to cats-effect ExitCase
   */
  final def tryToExitCase[A](ta: Try[A]): ExitCase =
    ta match {
      case Return(_) => ExitCase.Succeeded
      case Throw(e)  => ExitCase.Errored(e)
    }

  /**
   * Convert a twitter-util Try to cats-effect Outcome for Rerunnable
   */
  final def tryToRerunnableOutcome[A](ta: Try[A]): Outcome[Rerunnable, Throwable, A] =
    ta match {
      case Return(a) => Outcome.Succeeded(Rerunnable.const(a))
      case Throw(e)  => Outcome.Errored(e)
    }

  /**
   * Convert a twitter-util Try to cats-effect Outcome for Future
   */
  final def tryToFutureOutcome[A](ta: Try[A]): Outcome[Future, Throwable, A] =
    ta match {
      case Return(a) => Outcome.Succeeded(Future.value(a))
      case Throw(e)  => Outcome.Errored(e)
    }
}
