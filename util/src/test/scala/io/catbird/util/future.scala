package io.catbird.util

import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.discipline.{ MonoidTests, SemigroupTests }
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.{ Comonad, Eq }
import com.twitter.conversions.DurationOps._
import com.twitter.util.Future
import org.scalacheck.Arbitrary

class FutureSuite extends CatbirdSuite with FutureInstances with ArbitraryInstances with EqInstances {
  implicit val eqFutureInt: Eq[Future[Int]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureInt: Eq[Future[Future[Int]]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureFutureInt: Eq[Future[Future[Future[Int]]]] = futureEqWithFailure(1.second)
  implicit val eqFutureInt3: Eq[Future[(Int, Int, Int)]] = futureEqWithFailure(1.second)
  implicit val eqFutureEitherUnit: Eq[Future[Either[Throwable, Unit]]] = futureEqWithFailure(1.second)
  implicit val eqFutureEitherInt: Eq[Future[Either[Throwable, Int]]] = futureEqWithFailure(1.second)
  implicit val comonad: Comonad[Future] = futureComonad(1.second)
  implicit val eqFutureParInt: Eq[FuturePar[Int]] = futureParEqWithFailure(1.second)
  implicit val eqFutureParInt3: Eq[FuturePar[(Int, Int, Int)]] = futureParEqWithFailure(1.second)
  implicit def arbFuturePar[A](implicit A: Arbitrary[A]): Arbitrary[FuturePar[A]] =
    Arbitrary(A.arbitrary.map(value => FuturePar(Future.value(value))))

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", ComonadTests[Future].comonad[Int, Int, Int])
  checkAll("Future[Int]", FunctorTests[Future](comonad).functor[Int, Int, Int])
  checkAll("Future[Int]", SemigroupTests[Future[Int]](twitterFutureSemigroup[Int]).semigroup)
  checkAll("Future[Int]", MonoidTests[Future[Int]].monoid)
  checkAll("Future[Int]", ParallelTests[Future, FuturePar].parallel[Int, Int])
  checkAll("FuturePar[Int]", CommutativeApplicativeTests[FuturePar].commutativeApplicative[Int, Int, Int])
}
