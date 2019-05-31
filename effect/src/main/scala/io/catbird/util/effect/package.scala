package io.catbird.util

import cats.effect.{ Async, IO }
import com.twitter.util.{ Future, Return, Throw }
import java.lang.Throwable
import scala.util.{ Left, Right }

package object effect extends RerunnableInstances {
  def futureToAsync[F[_], A](fa: => Future[A])(implicit F: Async[F]): F[A] = F.async { k =>
    fa.respond {
      case Return(a)  => k(Right[Throwable, A](a))
      case Throw(err) => k(Left[Throwable, A](err))
    }
  }

  final def rerunnableToIO[A](fa: Rerunnable[A]): IO[A] = futureToAsync[IO, A](fa.run)
}
