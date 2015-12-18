package io.catbird

import cats.Eq

package object tests {
  implicit def eqTuple3[A, B, C](implicit A: Eq[A], B: Eq[B], C: Eq[C]): Eq[(A, B, C)] =
    Eq.instance {
      case ((a1, b1, c1), (a2, b2, c2)) => A.eqv(a1, a2) && B.eqv(b1, b2) && C.eqv(c1, c2)
    }
}
