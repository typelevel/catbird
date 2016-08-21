package io.catbird.util

import cats.data.Xor
import cats.kernel.laws.GroupLaws
import cats.laws.discipline.{ MonadErrorTests, TraverseTests }
import cats.laws.discipline.arbitrary._
import cats.instances.int._
import cats.instances.option._
import cats.instances.string._
import cats.instances.tuple._
import cats.instances.unit._
import com.twitter.util.{ Return, Throw, Try }
import io.catbird.bijections.util.TryConversions
import io.catbird.tests.EqInstances
import io.catbird.tests.bijection.BijectionProperties
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.prop.{ Checkers, PropertyChecks }
import org.scalatest.{ FunSuite, PropSpec }
import org.typelevel.discipline.scalatest.Discipline

trait TryTest extends ArbitraryInstances with EqInstances with TryConversions with TryInstances

class TrySuite extends FunSuite with TryTest with Discipline {
  checkAll("Try[Int]", MonadErrorTests[Try, Throwable].monadError[Int, Int, Int])
  checkAll("Try[Int]", TraverseTests[Try].traverse[Int, Int, Int, Int, Option, Option])
  checkAll("Try[Int]", GroupLaws[Try[Int]].semigroup(twitterTrySemigroup[Int]))
  checkAll("Try[Int]", GroupLaws[Try[Int]].monoid)
}

class TrySpec extends PropSpec with PropertyChecks with Checkers
  with TryTest with BijectionProperties {
  check {
    isBijection[Try[String], Xor[Throwable, String]]
  }

  property("Equality for Try should use universal equality for Throw") {
    case class SomeError(message: String) extends Exception(message)

    forAll((s: String) => assert(twitterTryEq[Int].eqv(Throw(SomeError(s)), Throw(SomeError(s)))))
  }

  property("Equality for Try should never mix up Return and Throw") {
    forAll((i: Int) => assert(!twitterTryEq[Int].eqv(Throw(new Exception), Return(i))))
  }
}
