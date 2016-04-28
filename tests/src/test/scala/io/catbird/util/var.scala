package io.catbird.util

import algebra.laws.GroupLaws
import algebra.std.tuple._
import cats.std.int._
import cats.{ Comonad, Eq }
import cats.laws.discipline._
import com.twitter.util.Var
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class VarSuite extends FunSuite with Discipline with VarInstances with ArbitraryInstances {
  implicit val eqVarInt: Eq[Var[Int]] = varEq
  implicit val eqVarVarInt: Eq[Var[Var[Int]]] = varEq
  implicit val eqVarVarVarInt: Eq[Var[Var[Var[Int]]]] = varEq
  implicit val eqVarInt3: Eq[Var[(Int, Int, Int)]] = varEq[(Int, Int, Int)]
  implicit val comonad: Comonad[Var] = varComonad

  checkAll("Var[Int]", MonadTests[Var].monad[Int, Int, Int])
  checkAll("Var[Int]", ComonadTests[Var].comonad[Int, Int, Int])
  checkAll("Var[Int]", GroupLaws[Var[Int]].semigroup(varSemigroup[Int]))
  checkAll("Var[Int]", GroupLaws[Var[Int]].monoid)
}
