package slick.codegen.patch

import cats.effect.IO
import cats.effect.kernel.Resource
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

trait FunctionalSpec extends UnitSpec {

  protected def slickTestKit(path: String): Resource[IO, SlickTestKit[JdbcProfile]] = {
    for {
      config <- createDatabaseConfig(path)
    } yield {
      SlickTestKit[JdbcProfile](config)
    }
  }

  private def createDatabaseConfig(path: String): Resource[IO, DatabaseConfig[JdbcProfile]] = {
    Resource.make(IO.delay {
      DatabaseConfig.forConfig[JdbcProfile](path = path)
    }) { config =>
      IO.delay {
        config.db.close
      }
    }
  }
}
