package slick.codegen.patch

import slick.model._
import slick.relational.RelationalProfile

sealed trait Patch extends Product with Serializable {
  def apply(model: Model): Model
  def andThen(other: Patch): Patch = this match {
    case Patch.Nope => other
    case self       => Patch.AndThen(self, other)
  }
}

object Patch {

  def apply(patches: Patch*): Patch =
    patches.reduceLeftOption(_ andThen _).getOrElse(Nope)

  sealed trait PatchTable extends Patch {
    def patch(table: Table): Table

    override def apply(model: Model): Model = model.copy(
      tables = model.tables.map(patch),
    )
    def onTable(cond: Table => Boolean): Patch = PatchTable.Conditional(cond, this)
    def onTables(names: String*): Patch = onTable {
      case TableRef(name) => names.contains(name)
      case _              => false
    }
  }

  object PatchTable {
    case class Conditional(cond: Table => Boolean, patch: PatchTable) extends PatchTable {
      private val p = patch

      override def patch(table: Table): Table = if (cond(table)) p.patch(table) else table
    }
  }

  case object DropSchema extends PatchTable {
    def patch(table: Table): Table = table.copy(
      name = dropSchema(table.name),
      columns = table.columns.map(dropSchema),
      primaryKey = table.primaryKey.map(dropSchema),
      foreignKeys = table.foreignKeys.map(dropSchema),
      indices = table.indices.map(dropSchema),
    )

    private def dropSchema(table: QualifiedName): QualifiedName = table.copy(schema = None)
    private def dropSchema(column: Column): Column              = column.copy(table = dropSchema(column.table))
    private def dropSchema(primaryKey: PrimaryKey): PrimaryKey = primaryKey.copy(
      table = dropSchema(primaryKey.table),
      columns = primaryKey.columns.map(dropSchema),
    )
    private def dropSchema(foreignKey: ForeignKey): ForeignKey = foreignKey.copy(
      referencingTable = dropSchema(foreignKey.referencingTable),
      referencingColumns = foreignKey.referencingColumns.map(dropSchema),
      referencedTable = dropSchema(foreignKey.referencedTable),
      referencedColumns = foreignKey.referencedColumns.map(dropSchema),
    )
    private def dropSchema(index: Index): Index = index.copy(
      table = dropSchema(index.table),
      columns = index.columns.map(dropSchema),
    )
  }

  case object DropDefaultValue extends PatchTable {
    override def patch(table: Table): Table = table.copy(
      columns = table.columns.map(dropDefaultValue),
      indices = table.indices.map { index =>
        index.copy(columns = index.columns.map(dropDefaultValue))
      },
      foreignKeys = table.foreignKeys.map { foreignKey =>
        foreignKey.copy(
          referencingColumns = foreignKey.referencingColumns.map(dropDefaultValue),
          referencedColumns = foreignKey.referencedColumns.map(dropDefaultValue),
        )
      },
    )
    private def dropDefaultValue(column: Column): Column = column.copy(
      options = column.options.filter {
        case RelationalProfile.ColumnOption.Default(_) => false
        case _                                         => true
      },
    )
  }

  case class AndThen(a: Patch, b: Patch) extends Patch {
    override def apply(model: Model): Model = b.apply(a.apply(model))
  }

  case class PatchColumn(f: Column => Column) extends PatchTable {
    override def patch(table: Table): Table = table.copy(
      columns = table.columns.map(refine),
      primaryKey = table.primaryKey.map(refine),
      foreignKeys = table.foreignKeys.map(refine),
      indices = table.indices.map(refine),
    )
    private def refine(column: Column): Column             = f(column)
    private def refine(primaryKey: PrimaryKey): PrimaryKey = primaryKey.copy(columns = primaryKey.columns.map(refine))
    private def refine(foreignKey: ForeignKey): ForeignKey = foreignKey.copy(
      referencingColumns = foreignKey.referencingColumns.map(refine),
      referencedColumns = foreignKey.referencedColumns.map(refine),
    )
    private def refine(index: Index): Index = index.copy(columns = index.columns.map(refine))
  }

  case class FilterTable(f: QualifiedName => Boolean) extends Patch {
    override def apply(model: Model): Model = model.copy(
      tables = model.tables.collect {
        case table if f(table.name) =>
          table.copy(
            foreignKeys = table.foreignKeys.filter { foreignKey =>
              f(foreignKey.referencedTable) && f(foreignKey.referencingTable)
            },
            indices = table.indices.filter { index =>
              f(index.table)
            },
          )
      },
    )
  }

  object FilterTable {
    def only(names: String*): FilterTable = FilterTable { name =>
      names.contains(name.table)
    }
    def drop(names: String*): FilterTable = FilterTable { name =>
      !names.contains(name.table)
    }
  }

  case class FilterColumn(f: Column => Boolean) extends PatchTable {
    override def patch(table: Table): Table = table.copy(
      columns = table.columns.filter(f),
      primaryKey = table.primaryKey.filter(pk => pk.columns.forall(f)),
      foreignKeys = table.foreignKeys.filter(fk => fk.referencedColumns.forall(f) && fk.referencingColumns.forall(f)),
      indices = table.indices.filter(index => index.columns.forall(f)),
    )
  }

  object FilterColumn {
    def drop(names: String*): FilterColumn = FilterColumn { column =>
      !names.contains(column.name)
    }
    def only(names: String*): FilterColumn = FilterColumn { column =>
      names.contains(column.name)
    }
  }

  case object Nope extends Patch {
    override def apply(model: Model): Model = model
  }

  object ColumnRef {
    def unapply(column: Column): Option[(String, String)] = Some(column.table.table -> column.name)
  }

  object TableRef {
    def unapply(table: Table): Option[String] = Some(table.name.table)
  }
}
