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

import cats.instances.int._
import cats.kernel.Eq
import cats.laws.discipline._
import cats.laws.discipline.eq._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Service
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class ServiceSuite
    extends AnyFunSuite
    with FunSuiteDiscipline
    with Configuration
    with ServiceInstances
    with ArbitraryInstances
    with EqInstances {
  implicit val eq: Eq[Service[Boolean, Int]] = serviceEq(1.second)

  checkAll("Service", CategoryTests[Service].compose[Boolean, Int, Boolean, Int])
  checkAll("Service", CategoryTests[Service].category[Boolean, Int, Boolean, Int])
  checkAll("Service", ProfunctorTests[Service].profunctor[Boolean, Int, Boolean, Int, Boolean, Int])
}
