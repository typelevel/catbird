package io.catbird.util

import algebra.laws.GroupLaws
import cats.data.Xor
import cats.laws.discipline.arbitrary._
import cats.std.int._
import cats.std.string._
import com.twitter.util.Try
import io.catbird.test.bijection.BijectionProperties
import io.catbird.test.util.ArbitraryInstances
import org.scalatest.prop.PropertyChecks
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class TryTests extends FunSuite with Discipline with BijectionProperties
  with ArbitraryInstances with TryConversions with TryInstances {
  test("Try[String] <=> Xor[Throwable, String]") {
    isBijection[Try[String], Xor[Throwable, String]]
  }

  checkAll("Try[Int]", GroupLaws[Try[Int]].semigroup(trySemigroup[Int]))
  checkAll("Try[Int]", GroupLaws[Try[Int]].monoid)
}
