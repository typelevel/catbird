package io.catbird.util

import cats.{ Comonad, Eq }
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.GroupLaws
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.time._
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
