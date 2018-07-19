package io.catbird.arrows

import _root_.arrows.twitter.Task
import cats.Comonad
import cats.data.EitherT
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.Eq
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.time._
import io.catbird.util.{ ArbitraryInstances, EqInstances }
import org.scalacheck.{ Arbitrary, Cogen }
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import cats.laws.discipline.SemigroupalTests.Isomorphisms

class RerunnableSuite extends FunSuite with Discipline with ArbitraryInstances with EqInstances {
  implicit def taskEq[A](implicit A: Eq[A]): Eq[Task[A]] =
    io.catbird.arrows.taskEqWithFailure[A](1.second)
  implicit val taskComonad: Comonad[Task] = io.catbird.arrows.taskComonad(1.second)

  implicit def taskArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Task[A]] =
    Arbitrary(A.arbitrary.map(Task.value))

  implicit def taskCogen[A](implicit A: Cogen[A]): Cogen[Task[A]] =
    A.contramap(taskComonad.extract)

  /**
   * I'm not sure why this can't be resolved implicitly (maybe the aliasness of
   * `Task`?).
   */
  implicit val eitherTEq: Eq[EitherT[Task, Throwable, Int]] =
    EitherT.catsDataEqForEitherT[Task, Throwable, Int]

  implicit val taskIsomorphisms: Isomorphisms[Task] = Isomorphisms.invariant[Task](taskComonad)

  checkAll("Task[Int]", MonadErrorTests[Task, Throwable].monadError[Int, Int, Int])
  checkAll("Task[Int]", ComonadTests[Task].comonad[Int, Int, Int])
  checkAll("Task[Int]", FunctorTests[Task](taskComonad).functor[Int, Int, Int])
  checkAll("Task[Int]", MonoidTests[Task[Int]].monoid)
}
