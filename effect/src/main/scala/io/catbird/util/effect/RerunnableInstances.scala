package io.catbird.util.effect

import cats.effect._
import com.twitter.util.{ Future, Promise }
import io.catbird.util._
import java.lang.Throwable
import scala.Unit
import scala.util.{ Either, Left, Right }

trait RerunnableInstances {
  implicit final val rerunnableEffectInstance: Effect[Rerunnable] = new RerunnableMonadError with Effect[Rerunnable] {
    final def suspend[A](thunk: => Rerunnable[A]): Rerunnable[A] = Rerunnable.suspend[A](thunk)

    override final def delay[A](thunk: => A): Rerunnable[A] = Rerunnable[A](thunk)

    final def async[A](k: (Either[Throwable, A] => Unit) => Unit): Rerunnable[A] = new Rerunnable[A] {
      final def run: Future[A] = {
        val promise = new Promise[A]

        k { e =>
          if (promise.isDefined) () else e match {
            case Right(a) => promise.setValue(a)
            case Left(err) => promise.setException(err)
          }
        }

        promise
      }
    }

    final def asyncF[A](k: (Either[Throwable, A] => Unit) => Rerunnable[Unit]): Rerunnable[A] = new Rerunnable[A] {
      final def run: Future[A] = {
        val promise = new Promise[A]

        k { e =>
          if (promise.isDefined) () else e match {
            case Right(a) => promise.setValue(a)
            case Left(err) => promise.setException(err)
          }
        }

        promise
      }
    }

    final def runAsync[A](fa: Rerunnable[A])(cb: Either[Throwable, A] => IO[Unit]): IO[Unit] =
      rerunnableToIO[A](fa).runAsync(cb)

    final def runSyncStep[A](fa: Rerunnable[A]): IO[Either[Rerunnable[A],A]] = rerunnableToIO(fa).map { Right(_) }

    final def bracketCase[A, B](acquire: io.catbird.util.Rerunnable[A])(use: A => io.catbird.util.Rerunnable[B])
      (release: (A, cats.effect.ExitCase[Throwable]) => io.catbird.util.Rerunnable[Unit]): io.catbird.util.Rerunnable[B] = {
      val bc = for {
        a <- acquire
        b <- use(a)
        _ <- release(a,ExitCase.Completed)
      } yield b
      bc
    }
  }
}
