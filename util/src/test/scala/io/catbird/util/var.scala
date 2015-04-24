package io.catbird.util

import algebra.laws.GroupLaws
import cats.std.int._
import cats.{ Comonad, Eq }
import cats.laws.discipline._
import com.twitter.util.Var
import io.catbird.test.util.ArbitraryKInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class VarSuite extends FunSuite with Discipline
  with VarInstances with ArbitraryKInstances {
  implicit val eqv: Eq[Var[Int]] = varEq
  implicit val comonad: Comonad[Var] = varComonad

  checkAll("Var[Int]", MonadTests[Var].monad[Int, Int, Int])
  checkAll("Var[Int]", ComonadTests[Var].comonad[Int, Int, Int])
  checkAll("Var[Int]", FunctorTests[Var](comonad).functor[Int, Int, Int])
  checkAll("Var[Int]", GroupLaws[Var[Int]].semigroup(varSemigroup[Int]))
  checkAll("Var[Int]", GroupLaws[Var[Int]].monoid)
}
