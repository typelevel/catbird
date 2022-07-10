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

import cats.effect.kernel.{ MonadCancel, Outcome }
import com.twitter.util.{ Future, Monitor }
import org.typelevel.catbird.util.FutureMonadError

import java.lang.Throwable

import scala.Unit

trait FutureInstances {
  implicit final val futureMonadCancelInstance
    : MonadCancel[Future, Throwable] with MonadCancel.Uncancelable[Future, Throwable] =
    new FutureMonadError with MonadCancel[Future, Throwable] with MonadCancel.Uncancelable[Future, Throwable] {

      final override def forceR[A, B](fa: Future[A])(fb: Future[B]): Future[B] =
        fa.liftToTry.flatMap { resultA =>
          resultA.handle[Any](Monitor.catcher)
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
