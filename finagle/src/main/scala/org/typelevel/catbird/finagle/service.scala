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

package org.typelevel.catbird.finagle

import cats.arrow.{ Category, Profunctor }
import org.typelevel.catbird.util.twitterFutureInstance
import com.twitter.finagle.Service

trait ServiceInstances {
  implicit val serviceInstance: Category[Service] with Profunctor[Service] =
    new Category[Service] with Profunctor[Service] {
      final def id[A]: Service[A, A] = Service.mk(twitterFutureInstance.pure)

      final def compose[A, B, C](f: Service[B, C], g: Service[A, B]): Service[A, C] =
        Service.mk(a => g(a).flatMap(f))

      final def dimap[A, B, C, D](fab: Service[A, B])(f: C => A)(g: B => D): Service[C, D] =
        Service.mk(c => fab.map(f)(c).map(g))

      override final def lmap[A, B, C](fab: Service[A, B])(f: C => A): Service[C, B] = fab.map(f)
    }
}
