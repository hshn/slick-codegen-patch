package slick.codegen.patch

import cats.effect.IO
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.model.Model

case class SlickTestKit[P <: JdbcProfile](
  slick: DatabaseConfig[P],
) {
  def createModel(ignoreInvalidDefaults: Boolean = true): IO[Model] = {
    fromFuture { implicit ec =>
      slick.db.run(slick.profile.createModel(None, ignoreInvalidDefaults = ignoreInvalidDefaults))
    }
  }

  private def fromFuture[A](f: ExecutionContext => Future[A]): IO[A] =
    IO.executionContext.flatMap(ec => IO.fromFuture(IO.delay(f(ec))))
}
