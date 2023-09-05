package slick.codegen.patch

import cats.effect.IO
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.jdbc.JdbcProfile
import slick.model.Model

case class SlickTestKit[P <: JdbcProfile](
  profile: P,
  db: P#Backend#Database,
) {
  def createModel(ignoreInvalidDefaults: Boolean = true): IO[Model] = {
    fromFuture { implicit ec =>
      db.run(profile.createModel(None, ignoreInvalidDefaults = ignoreInvalidDefaults))
    }
  }

  private def fromFuture[A](f: ExecutionContext => Future[A]): IO[A] =
    IO.executionContext.flatMap(ec => IO.fromFuture(IO.delay(f(ec))))
}
