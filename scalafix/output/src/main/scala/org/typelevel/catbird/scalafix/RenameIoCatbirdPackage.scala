package org.typelevel.catbird.scalafix

import cats.MonadError
import com.twitter.util.{ Await, Future }
import org.typelevel.catbird.util._

object RenameIoCatbirdPackage {
  def apply()(implicit F: MonadError[Future, Throwable]): Future[Unit] =
    F.raiseError[Unit](new NotImplementedError)
}

object Main {
  Await.result(RenameIoCatbirdPackage())
}
