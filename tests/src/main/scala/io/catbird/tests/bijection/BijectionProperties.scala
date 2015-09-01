package io.catbird.tests.bijection

import cats.Eq
import com.twitter.bijection.{ Bijection, ImplicitBijection, Injection }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll

/**
 * Adapted from bijection-core's tests, which aren't published.
 */
trait BijectionProperties {
  def rt[A, B](a: A)(implicit bij: Bijection[A, B]): A = rtInjective[A, B](a)
  def rtInjective[A, B](a: A)(implicit bij: Bijection[A, B]): A = bij.invert(bij(a))

  def isInjective[A, B](implicit
    a: Arbitrary[A],
    bij: ImplicitBijection[A, B],
    eqA: Eq[A]
  ): Prop =
    forAll { (a: A) =>
      eqA.eqv(a, rt(a)(bij.bijection))
    }

  def invertIsInjection[A, B](implicit
    b: Arbitrary[B],
    bij: ImplicitBijection[A, B],
    eqB: Eq[B]
  ): Prop =
    forAll { (b: B) =>
      eqB.eqv(b, rtInjective(b)(bij.bijection.inverse))
    }

  def isBijection[A, B](implicit
    arbA: Arbitrary[A],
    arbB: Arbitrary[B],
    bij: ImplicitBijection[A, B],
    eqA: Eq[A],
    eqB: Eq[B]
  ): Prop = {
    implicit val inj = Injection.fromBijection(bij.bijection)
    isInjective[A, B] && invertIsInjection[A, B]
  }
}
