package io.catbird.util

import algebra.laws.GroupLaws
import cats.std.int._
import cats.{ Comonad, Eq }
import cats.laws.discipline._
import cats.laws.discipline.eq._
import com.twitter.conversions.time._
import com.twitter.util.Future
import io.catbird.tests._
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class FutureSuite extends FunSuite with Discipline with FutureInstances with ArbitraryInstances {
  implicit val eqFutureInt: Eq[Future[Int]] = futureEq(1.second)
  implicit val eqFutureFutureInt: Eq[Future[Future[Int]]] = futureEq(1.second)
  implicit val eqFutureFutureFutureInt: Eq[Future[Future[Future[Int]]]] = futureEq(1.second)
  implicit val eqFutureInt3: Eq[Future[(Int, Int, Int)]] = futureEq[(Int, Int, Int)](1.second)
  implicit val comonad: Comonad[Future] = futureComonad(1.second)

  checkAll("Future[Int]", MonadTests[Future].monad[Int, Int, Int])
  checkAll("Future[Int]", ComonadTests[Future].comonad[Int, Int, Int])
  checkAll("Future[Int]", FunctorTests[Future](comonad).functor[Int, Int, Int])
  checkAll("Future[Int]", GroupLaws[Future[Int]].semigroup(futureSemigroup[Int]))
  checkAll("Future[Int]", GroupLaws[Future[Int]].monoid)
}
