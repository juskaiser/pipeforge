package io.phdata.jdbc.parsing

import java.sql._

import com.typesafe.scalalogging.LazyLogging
import io.phdata.jdbc.config.{DatabaseConf, DatabaseType, ObjectType}
import io.phdata.jdbc.domain.{Column, Table}

import scala.util.{Failure, Success, Try}

trait DatabaseMetadataParser extends LazyLogging {

  def connection: Connection

  def listTablesStatement(schema: String): String

  def singleRecordQuery(schema: String, table: String): String

  def listViewsStatement(schema: String): String

  def getColumnDefinitions(schema: String, table: String): Set[Column]

  def getTablesMetadata(objectType: ObjectType.Value,
                        schema: String,
                        tableWhiteList: Option[Set[String]]): Try[Set[Table]] = {
    val sourceTables = listTables(objectType, schema)
    checkWhiteListedTables(sourceTables, tableWhiteList) match {
      case Success(tables) => Try(tables.map(getTableMetadata(schema, _)))
      case Failure(ex) => Failure(ex)
    }
  }

  def checkWhiteListedTables(sourceTables: Set[String], tableWhiteList: Option[Set[String]]): Try[Set[String]] = {
    tableWhiteList match {
      case Some(whiteList) =>
        if (whiteList.subsetOf(sourceTables)) {
          Success(whiteList)
        } else {
          Failure(new Exception(s"A table in the whitelist was not found in the source system, whitelist=$whiteList, source tables=$sourceTables"))
        }
      case None => Success(sourceTables)
    }
  }

  def getTableMetadata(schema: String, table: String): Table = {
    val allColumns = getColumnDefinitions(schema, table)
    val pks = primaryKeys(schema, table, allColumns)
    val columns = allColumns.diff(pks)
    Table(table, pks, columns)
  }

  def primaryKeys(schema: String, table: String, columns: Set[Column]): Set[Column] = {
    val rs: ResultSet = metadata.getPrimaryKeys(schema, schema, table)
    val pks = results(rs) { record =>
      record.getString("COLUMN_NAME") -> record.getInt("KEY_SEQ")
    }.toMap

    mapPrimaryKeyToColumn(pks, columns)
  }

  def mapMetaDataToColumn(metaData: ResultSetMetaData, rsMetadata: ResultSetMetaData): Set[Column] = {
    def asBoolean(i: Int) = if (i == 0) false else true

    (1 to metaData.getColumnCount).map { i =>
      Column(
        metaData.getColumnName(i),
        JDBCType.valueOf(rsMetadata.getColumnType(i)),
        asBoolean(metaData.isNullable(i)),
        i,
        metaData.getPrecision(i),
        metaData.getScale(i)
      )
    }.toSet
  }

  def mapPrimaryKeyToColumn(primaryKeys: Map[String, Int], columns: Set[Column]) = {
    primaryKeys.flatMap {
      case (key, index) =>
        columns.find(_.name == key) match {
          case Some(column) => Some(column)
          case None => None
        }
    }.toSet
  }

  def metadata = connection.getMetaData

  def listTables(objectType: ObjectType.Value, schema: String): Set[String] = {
    val stmt: Statement = newStatement
    val query =
      if (objectType == ObjectType.TABLE) listTablesStatement(schema)
      else listViewsStatement(schema)
    logger.debug("Executing query: {}", query)
    results(stmt.executeQuery(query))(_.getString(1)).toSet
  }

  def newStatement = connection.createStatement()

  protected def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()

      def next() = f(resultSet)
    }
  }
}

object DatabaseMetadataParser extends LazyLogging {
  def parse(configuration: DatabaseConf): Try[Set[Table]] = {
    logger.info("Extracting metadata information: {}", configuration)

    getConnection(configuration) match {
      case Success(connection) =>
        configuration.databaseType match {
          case DatabaseType.MYSQL =>
            new MySQLMetadataParser(connection)
              .getTablesMetadata(configuration.objectType, configuration.schema, configuration.tables)
          case DatabaseType.ORACLE =>
            new OracleMetadataParser(connection)
              .getTablesMetadata(configuration.objectType, configuration.schema, configuration.tables)
          case DatabaseType.MSSQL =>
            new MsSQLMetadataParser(connection)
              .getTablesMetadata(configuration.objectType, configuration.schema, configuration.tables)
          case _ =>
              Failure(
                new Exception(s"Metadata parser for database type: " +
                  s"${configuration.databaseType} has not been configured"))
        }
      case Failure(e) =>
        logger.error(s"Failed connecting to: $configuration", e)
        throw e
    }
  }

  def getConnection(configuration: DatabaseConf) = {
    logger.info(configuration.jdbcUrl)
    Try(
      DriverManager.getConnection(configuration.jdbcUrl,
        configuration.username,
        configuration.password))
  }

}
