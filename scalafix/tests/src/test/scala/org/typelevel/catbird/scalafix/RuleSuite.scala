package org.typelevel.catbird.scalafix

import scalafix.testkit._
import org.scalatest.funsuite.AnyFunSuiteLike

class RuleSuite extends AbstractSemanticRuleSuite with AnyFunSuiteLike {
  runAllTests()
}
