/*
 * Copyright 2022 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.typelevel.catbird.util.effect

import cats.effect.{ Effect, ExitCase, IO, SyncIO }
import com.twitter.util.{ Future, Monitor, Promise }
import org.typelevel.catbird.util.{ Rerunnable, RerunnableMonadError }
import java.lang.Throwable
import scala.Unit
import scala.util.{ Either, Left, Right }

trait RerunnableInstances {
  implicit final val rerunnableEffectInstance: Effect[Rerunnable] =
    new RerunnableMonadError with Effect[Rerunnable] {
      final def suspend[A](thunk: => Rerunnable[A]): Rerunnable[A] = Rerunnable.suspend[A](thunk)

      override final def delay[A](thunk: => A): Rerunnable[A] = Rerunnable[A](thunk)

      final def async[A](k: (Either[Throwable, A] => Unit) => Unit): Rerunnable[A] =
        new Rerunnable[A] {
          final def run: Future[A] = {
            val promise = new Promise[A]

            k { e =>
              if (promise.isDefined) ()
              else
                e match {
                  case Right(a)  => promise.setValue(a)
                  case Left(err) => promise.setException(err)
                }
            }

            promise
          }
        }

      final def asyncF[A](k: (Either[Throwable, A] => Unit) => Rerunnable[Unit]): Rerunnable[A] =
        new Rerunnable[A] {
          final def run: Future[A] = {
            val promise = new Promise[A]

            val rerunnable = k { e =>
              if (promise.isDefined) ()
              else
                e match {
                  case Right(a)  => promise.setValue(a)
                  case Left(err) => promise.setException(err)
                }
            }

            rerunnable.run.flatMap(_ => promise)
          }
        }

      final def runAsync[A](fa: Rerunnable[A])(cb: Either[Throwable, A] => IO[Unit]): SyncIO[Unit] =
        rerunnableToIO[A](fa).runAsync(cb)

      final def bracketCase[A, B](acquire: Rerunnable[A])(use: A => Rerunnable[B])(
        release: (A, ExitCase[Throwable]) => Rerunnable[Unit]
      ): Rerunnable[B] = new Rerunnable[B] {
        final def run: Future[B] =
          acquire.run.flatMap { a =>
            val future = use(a).run
            future.transform(result =>
              release(a, tryToExitCase(result)).run.handle(Monitor.catcher).flatMap(_ => future)
            )
          }
      }
    }
}
