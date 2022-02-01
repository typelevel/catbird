package org.typelevel.catbird.scalafix

import scalafix.v1._

class CatbirdPackageRename extends SemanticRule("CatbirdPackageRename") {
  override def fix(implicit doc: SemanticDocument): Patch =
    Patch.replaceSymbols("io.catbird" -> "org.typelevel.catbird")
}
