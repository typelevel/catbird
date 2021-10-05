package org.typelevel.catbird.util.effect

import cats.effect.Clock
import cats.effect.kernel.{ MonadCancel, Outcome, Sync }
import com.twitter.util.{ Future, Monitor }
import org.typelevel.catbird.util.{ Rerunnable, RerunnableMonadError }

import java.lang.Throwable
import java.util.concurrent.TimeUnit
import java.lang.System

import scala.Unit
import scala.concurrent.duration.FiniteDuration

trait RerunnableInstances {
  implicit final val rerunnableInstance
    : Sync[Rerunnable] with Clock[Rerunnable] with MonadCancel.Uncancelable[Rerunnable, Throwable] =
    new RerunnableMonadError
      with Sync[Rerunnable]
      with Clock[Rerunnable]
      with MonadCancel.Uncancelable[Rerunnable, Throwable] {

      final override def suspend[A](hint: Sync.Type)(thunk: => A): Rerunnable[A] =
        Rerunnable(thunk)

      final override def realTime: Rerunnable[FiniteDuration] =
        Rerunnable(FiniteDuration(System.currentTimeMillis(), TimeUnit.MILLISECONDS))

      final override def monotonic: Rerunnable[FiniteDuration] =
        Rerunnable(FiniteDuration(System.nanoTime(), TimeUnit.NANOSECONDS))

      final override def forceR[A, B](fa: Rerunnable[A])(fb: Rerunnable[B]): Rerunnable[B] =
        fa.liftToTry.flatMap { resultA =>
          resultA.handle(Monitor.catcher)
          fb
        }

      /**
       * Special implementation so exceptions in release are cought by the `Monitor`.
       */
      final override def bracketCase[A, B](acquire: Rerunnable[A])(use: A => Rerunnable[B])(
        release: (A, Outcome[Rerunnable, Throwable, B]) => Rerunnable[Unit]
      ): Rerunnable[B] = new Rerunnable[B] {
        final def run: Future[B] =
          acquire.run.flatMap { a =>
            val future = use(a).run
            future.transform(result =>
              release(a, tryToRerunnableOutcome(result)).run.handle(Monitor.catcher).flatMap(_ => future)
            )
          }
      }
    }
}
