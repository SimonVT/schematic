/*
 * Copyright (C) 2014 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.schematic.sample.database

import net.simonvt.schematic.annotation.AutoIncrement
import net.simonvt.schematic.annotation.Check
import net.simonvt.schematic.annotation.DataType
import net.simonvt.schematic.annotation.DataType.Type.INTEGER
import net.simonvt.schematic.annotation.DataType.Type.TEXT
import net.simonvt.schematic.annotation.PrimaryKey
import net.simonvt.schematic.annotation.References
import net.simonvt.schematic.sample.database.NotesDatabase.Tables

interface NoteColumns {
  companion object {
    const val STATUS_NEW = "new"
    const val STATUS_COMPLETED = "completed"

    @DataType(INTEGER) @PrimaryKey @AutoIncrement const val ID = "_id"

    @DataType(INTEGER) @References(table = Tables.LISTS, column = ListColumns.ID)
    const val LIST_ID = "listId"

    @DataType(TEXT) const val NOTE = "note"

    @DataType(TEXT)
    @Check(NoteColumns.STATUS + " in ('" + STATUS_NEW + "', '" + STATUS_COMPLETED + "')")
    const val STATUS = "status"
  }

}
