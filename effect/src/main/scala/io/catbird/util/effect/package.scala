package io.catbird.util

import cats.effect.IO
import com.twitter.util.{ Return, Throw }
import java.lang.Throwable
import scala.util.{ Left, Right }

package object effect extends RerunnableInstances {
  final def rerunnableToIO[A](fa: Rerunnable[A]): IO[A] = IO.async { k =>
    fa.run.respond {
      case Return(a) => k(Right[Throwable, A](a))
      case Throw(err) => k(Left[Throwable, A](err))
    }
  }
}
