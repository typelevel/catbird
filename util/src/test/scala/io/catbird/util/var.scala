package io.catbird.util

import cats.instances.int._
import cats.instances.tuple._
import cats.kernel.laws.discipline.{ MonoidTests, SemigroupTests }
import cats.laws.discipline._
import cats.{ Comonad, Eq }
import com.twitter.util.Var

class VarSuite extends CatbirdSuite with VarInstances with ArbitraryInstances {
  implicit val eqVarInt: Eq[Var[Int]] = varEq
  implicit val eqVarVarInt: Eq[Var[Var[Int]]] = varEq
  implicit val eqVarVarVarInt: Eq[Var[Var[Var[Int]]]] = varEq
  implicit val eqVarInt3: Eq[Var[(Int, Int, Int)]] = varEq[(Int, Int, Int)]
  implicit val comonad: Comonad[Var] = varComonad

  checkAll("Var[Int]", MonadTests[Var].stackUnsafeMonad[Int, Int, Int])
  checkAll("Var[Int]", ComonadTests[Var].comonad[Int, Int, Int])
  checkAll("Var[Int]", SemigroupTests[Var[Int]](twitterVarSemigroup[Int]).semigroup)
  checkAll("Var[Int]", MonoidTests[Var[Int]].monoid)
}
