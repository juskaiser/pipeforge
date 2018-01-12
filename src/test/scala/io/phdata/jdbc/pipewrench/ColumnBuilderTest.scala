/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.jdbc.pipewrench

import java.sql.JDBCType

import io.phdata.jdbc.domain.Column
import org.scalatest.FunSuite

/**
  * ColumnBuilder unit tests
  */
class ColumnBuilderTest extends FunSuite {
  test("map decimal") {
    val column = Column("col1", JDBCType.NUMERIC, false, 0, 10, 4)
    assertResult(DataType.DECIMAL)(ColumnBuilder.mapDataType(column))
  }

  test("map big decimal") {
    val column = Column("col1", JDBCType.NUMERIC, false, 0, 19, 0)
    assertResult(DataType.BIG_INT)(ColumnBuilder.mapDataType(column))
  }

  test("map int") {
    val column = Column("col1", JDBCType.NUMERIC, false, 0, 9, 0)
    assertResult(DataType.INTEGER)(ColumnBuilder.mapDataType(column))
  }

  test("map short") {
    val column = Column("col1", JDBCType.NUMERIC, false, 0, 5, 0)
    assertResult(DataType.SHORT)(ColumnBuilder.mapDataType(column))
  }

  test("map boolean") {
    val column = Column("col1", JDBCType.BOOLEAN, false, 0, 0, 0)
    assertResult(DataType.BOOLEAN.toString)(ColumnBuilder.mapDataType(column))
  }
}
