package io.catbird.util

import algebra.laws.GroupLaws
import cats.{ Comonad, Eq }
import cats.laws.discipline._
import cats.laws.discipline.eq._
import cats.std.int._
import com.twitter.conversions.time._
import io.catbird.tests.EqInstances
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class RerunnableSuite extends FunSuite with Discipline with ArbitraryInstances with EqInstances {
  implicit def rerunnableEq[A](implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Rerunnable.rerunnableEqWithFailure[A](1.second)
  implicit val rerunnableComonad: Comonad[Rerunnable] = Rerunnable.rerunnableComonad(1.second)

  checkAll("Rerunnable[Int]", MonadErrorTests[Rerunnable, Throwable].monadError[Int, Int, Int])
  checkAll("Rerunnable[Int]", ComonadTests[Rerunnable].comonad[Int, Int, Int])
  checkAll("Rerunnable[Int]", FunctorTests[Rerunnable](rerunnableComonad).functor[Int, Int, Int])
  checkAll("Rerunnable[Int]", GroupLaws[Rerunnable[Int]].semigroup)
  checkAll("Rerunnable[Int]", GroupLaws[Rerunnable[Int]].monoid)
}
