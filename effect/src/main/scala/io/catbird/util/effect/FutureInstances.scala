package io.catbird.util.effect

import cats.effect.{ Bracket, ExitCase }
import com.twitter.util.{ Future, Monitor }
import io.catbird.util.FutureMonadError
import java.lang.Throwable
import scala.Unit

trait FutureInstances {
  implicit final val futureBracketInstance: Bracket[Future, Throwable] =
    new FutureMonadError with Bracket[Future, Throwable] {
      final def bracketCase[A, B](acquire: Future[A])(use: A => Future[B])(
        release: (A, ExitCase[Throwable]) => Future[Unit]
      ): Future[B] =
        acquire
          .flatMap(a =>
            use(a).liftToTry
              .flatMap(result => release(a, tryToExitCase(result)).handle(Monitor.catcher).map(_ => result))
          )
          .lowerFromTry
    }
}
