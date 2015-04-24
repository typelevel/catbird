package io.catbird.util

import algebra.laws.GroupLaws
import cats.std.int._
import cats.{ Comonad, Eq }
import cats.laws.discipline._
import com.twitter.conversions.time._
import com.twitter.util.Future
import io.catbird.test.util.ArbitraryKInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class FutureSuite extends FunSuite with Discipline
  with FutureInstances with ArbitraryKInstances {
  implicit val eqv: Eq[Future[Int]] = futureEq(1.second)
  implicit val comonad: Comonad[Future] = futureComonad(1.second)

  checkAll("Future[Int]", MonadTests[Future].monad[Int, Int, Int])
  checkAll("Future[Int]", ComonadTests[Future].comonad[Int, Int, Int])
  checkAll("Future[Int]", FunctorTests[Future](comonad).functor[Int, Int, Int])
  checkAll("Future[Int]", GroupLaws[Future[Int]].semigroup(futureSemigroup[Int]))
  checkAll("Future[Int]", GroupLaws[Future[Int]].monoid)
}
