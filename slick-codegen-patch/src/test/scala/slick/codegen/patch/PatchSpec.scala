package slick.codegen.patch

import cats.effect.IO
import cats.implicits._
import slick.codegen.patch.Patch._
import slick.model.Model
import slick.relational.RelationalProfile

class PatchSpec extends FunctionalSpec {
  "Patch" can {
    "DropSchema" should {
      "Update table schema to None" in {
        testPatch(Patch.DropSchema) { model =>
          model.tables.foreach { table =>
            table.name.schema shouldEqual "default".some
          }
        } { patched =>
          patched.tables.foreach { table =>
            table.name.schema shouldEqual None
          }
        }
      }
    }
    "DropDefaultValue" should {
      "remove default value from columns" in {
        testPatch(Patch.DropDefaultValue) { model =>
          model.tables.exists { table =>
            table.columns.exists { column =>
              column.options.exists {
                case _: RelationalProfile.ColumnOption.Default[_] => true
                case _                                            => false
              }
            }
          } shouldBe true
        } { patched =>
          patched.tables.exists { table =>
            table.columns.exists { column =>
              column.options.exists {
                case _: RelationalProfile.ColumnOption.Default[_] => true
                case _                                            => false
              }
            }
          } shouldBe false
        }
      }
    }
    "FilterTable" should {
      "remove specified table and related columns" in {
        val patch = FilterTable.only("users", "posts")

        testPatch(patch) { model =>
          model.tables.map(_.name.table) should contain theSameElementsAs Seq(
            "flyway_schema_history",
            "users",
            "posts",
            "tags",
            "post_tags",
          )
        } { patched =>
          patched.tables.map(_.name.table) should contain theSameElementsAs Seq(
            "users",
            "posts",
          )
        }
      }
    }
    "FilterColumn" should {
      "remove specified column(s)" in {
        val patch = Patch(
          FilterTable.drop("flyway_schema_history"),
          FilterColumn.drop("created_at", "updated_at"),
        )

        testPatch(patch) { model =>
          model.tables
            .filter { table =>
              table.name.table != "flyway_schema_history"
            }
            .foreach { table =>
              table.columns.map(_.name) should contain allOf ("created_at", "updated_at")
            }
        } { patched =>
          patched.tables.foreach { table =>
            table.columns.map(_.name) should contain noneOf ("created_at", "updated_at")
          }
        }
      }
    }
    "PatchTable#onTables" should {
      "limit tables that a patch apply to" in {
        val patch = Patch(
          FilterColumn.drop("created_at").onTables("users"),
          FilterColumn.drop("updated_at").onTables("posts"),
        )

        testPatch(patch) { model =>
          model.tables.find(_.name.table == "users").value.columns.find(_.name == "created_at") shouldBe defined
          model.tables.find(_.name.table == "posts").value.columns.find(_.name == "updated_at") shouldBe defined
        } { patched =>
          patched.tables.find(_.name.table == "users").value.columns.find(_.name == "created_at") shouldBe empty
          patched.tables.find(_.name.table == "users").value.columns.find(_.name == "updated_at") shouldBe defined
          patched.tables.find(_.name.table == "posts").value.columns.find(_.name == "created_at") shouldBe defined
          patched.tables.find(_.name.table == "posts").value.columns.find(_.name == "updated_at") shouldBe empty
        }
      }
    }
    "PatchColumn" should {
      "patch column" in {
        val patch = PatchColumn {
          case column @ ColumnRef("users", "id")      => column.copy(tpe = "UserId")
          case column @ ColumnRef("posts", "user_id") => column.copy(tpe = "UserId")
          case column                                 => column
        }

        testPatch(patch) { model =>
          model.tables.find(_.name.table == "users").value.columns.find(_.name == "id").value.tpe shouldEqual "String"
          model.tables.find(_.name.table == "posts").value.columns.find(_.name == "user_id").value.tpe shouldEqual "String"
        } { patched =>
          patched.tables.find(_.name.table == "users").value.columns.find(_.name == "id").value.tpe shouldEqual "UserId"
          patched.tables.find(_.name.table == "posts").value.columns.find(_.name == "user_id").value.tpe shouldEqual "UserId"
        }
      }
    }
  }

  private def testPatch(
    patch: Patch,
    configPatch: String = "slick.mysql-default",
  )(precondition: Model => Any)(test: Model => Any): IO[Unit] =
    slickTestKit(configPatch).use { testKit =>
      for {
        model <- testKit.createModel()
      } yield {
        precondition(model)
        val patched = patch(model)

        patched.assertConsistency()

        test(patched)

        ()
      }
    }
}
