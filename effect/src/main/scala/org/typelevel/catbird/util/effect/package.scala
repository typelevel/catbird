package org.typelevel.catbird.util

import cats.effect.{ Async, ContextShift, ExitCase, IO }
import com.twitter.util.{ Future, Return, Throw, Try }
import java.lang.Throwable

import scala.util.{ Left, Right }

package object effect extends FutureInstances with RerunnableInstances {

  /**
   * Converts the `Future` to `F` without changing the underlying execution (same thread pool!).
   */
  def futureToAsync[F[_], A](fa: => Future[A])(implicit F: Async[F]): F[A] = F.async { k =>
    fa.respond {
      case Return(a)  => k(Right[Throwable, A](a))
      case Throw(err) => k(Left[Throwable, A](err))
    }

    ()
  }

  /**
   * The same as `futureToAsync` but doesn't stay on the thread pool of the `Future` and instead shifts execution
   * back to the one provided by `ContextShift[F]` (which is usually the default one).
   *
   * This is likely what you want when you interact with libraries that return a `Future` like `finagle-http` where
   * the `Future` is running on a thread pool controlled by the library (e.g. the underlying Netty pool).
   * It also is closer to the behavior of `IO.fromFuture` for Scala futures which also shifts back.
   */
  def futureToAsyncAndShift[F[_], A](fa: => Future[A])(implicit F: Async[F], CS: ContextShift[F]): F[A] =
    F.guarantee(futureToAsync[F, A](fa))(CS.shift)

  /**
   * Converts the `Rerunnable` to `F` without changing the underlying execution (same thread pool!).
   */
  final def rerunnableToIO[A](fa: Rerunnable[A]): IO[A] =
    futureToAsync[IO, A](fa.run)

  /**
   * The same as `rerunnableToIO` but doesn't stay on the thread pool of the `Rerunnable` and instead shifts execution
   * back to the one provided by `ContextShift[F]` (which is usually the default one).
   */
  final def rerunnableToIOAndShift[A](fa: Rerunnable[A])(implicit CS: ContextShift[IO]): IO[A] =
    futureToAsyncAndShift[IO, A](fa.run)

  /**
   * Convert a twitter-util Try to cats-effect ExitCase
   */
  final def tryToExitCase[A](ta: Try[A]): ExitCase[Throwable] =
    ta match {
      case Return(_) => ExitCase.complete
      case Throw(e)  => ExitCase.error(e)
    }
}
