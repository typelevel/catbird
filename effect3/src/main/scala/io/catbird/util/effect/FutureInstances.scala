package io.catbird.util.effect

import cats.effect.kernel.{ MonadCancel, Outcome }
import com.twitter.util.{ Future, Monitor }
import io.catbird.util.FutureMonadError

import java.lang.Throwable

import scala.Unit

trait FutureInstances {
  implicit final val futureMonadCancelInstance
    : MonadCancel[Future, Throwable] with MonadCancel.Uncancelable[Future, Throwable] =
    new FutureMonadError with MonadCancel[Future, Throwable] with MonadCancel.Uncancelable[Future, Throwable] {

      final override def forceR[A, B](fa: Future[A])(fb: Future[B]): Future[B] =
        fa.liftToTry.flatMap { resultA =>
          resultA.handle(Monitor.catcher)
          fb
        }

      /**
       * Special implementation so exceptions in release are cought by the `Monitor`.
       */
      final override def bracketCase[A, B](acquire: Future[A])(use: A => Future[B])(
        release: (A, Outcome[Future, Throwable, B]) => Future[Unit]
      ): Future[B] =
        acquire
          .flatMap(a =>
            use(a).liftToTry
              .flatMap(result => release(a, tryToFutureOutcome(result)).handle(Monitor.catcher).map(_ => result))
          )
          .lowerFromTry
    }
}
