package io.catbird.util

import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.{ Comonad, Eq }
import com.twitter.conversions.DurationOps._
import org.scalacheck.Arbitrary

class RerunnableSuite extends CatbirdSuite with ArbitraryInstances with EqInstances {
  implicit def rerunnableEq[A](implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Rerunnable.rerunnableEqWithFailure[A](1.second)
  implicit val rerunnableComonad: Comonad[Rerunnable] =
    Rerunnable.rerunnableComonad(1.second)
  implicit val rerunnableParEqInt: Eq[Rerunnable.Par[Int]] =
    Rerunnable.rerunnableParEqWithFailure(1.second)
  implicit val rerunnableParEqInt3: Eq[Rerunnable.Par[(Int, Int, Int)]] =
    Rerunnable.rerunnableParEqWithFailure(1.second)
  implicit def rerunnableParArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Rerunnable.Par[A]] =
    Arbitrary(A.arbitrary.map(value => Rerunnable.Par(Rerunnable.const(value))))

  checkAll("Rerunnable[Int]", MonadErrorTests[Rerunnable, Throwable].monadError[Int, Int, Int])
  checkAll("Rerunnable[Int]", ComonadTests[Rerunnable].comonad[Int, Int, Int])
  checkAll("Rerunnable[Int]", FunctorTests[Rerunnable](rerunnableComonad).functor[Int, Int, Int])
  checkAll("Rerunnable[Int]", MonoidTests[Rerunnable[Int]].monoid)
  checkAll("Rerunnable[Int]", ParallelTests[Rerunnable, Rerunnable.Par].parallel[Int, Int])
  checkAll("Rerunnable.Par[Int]", CommutativeApplicativeTests[Rerunnable.Par].commutativeApplicative[Int, Int, Int])
}
