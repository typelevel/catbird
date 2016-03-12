package io.catbird.util

import algebra.laws.GroupLaws
import cats.data.Xor
import cats.laws.discipline.arbitrary._
import cats.std.int._
import cats.std.string._
import com.twitter.util.{ Return, Throw, Try }
import io.catbird.bijections.util.TryConversions
import io.catbird.tests.bijection.BijectionProperties
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.prop.{ Checkers, PropertyChecks }
import org.scalatest.{ FunSuite, PropSpec }
import org.typelevel.discipline.scalatest.Discipline

trait TryTest extends ArbitraryInstances with TryConversions with TryInstances

class TrySuite extends FunSuite with TryTest with Discipline {
  checkAll("Try[Int]", GroupLaws[Try[Int]].semigroup(trySemigroup[Int]))
  checkAll("Try[Int]", GroupLaws[Try[Int]].monoid)
}

class TrySpec extends PropSpec with PropertyChecks with Checkers
  with TryTest with BijectionProperties {
  check {
    isBijection[Try[String], Xor[Throwable, String]]
  }

  property("Equality for Try should use universal equality for Throw") {
    case class SomeError(message: String) extends Exception(message)

    forAll((s: String) => assert(tryEq[Int].eqv(Throw(SomeError(s)), Throw(SomeError(s)))))
  }

  property("Equality for Try should never mix up Return and Throw") {
    forAll((i: Int) => assert(!tryEq[Int].eqv(Throw(new Exception), Return(i))))
  }
}
