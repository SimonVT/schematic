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

package net.simonvt.schematic.sample.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.OnConfigure;
import net.simonvt.schematic.annotation.OnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(version = NotesDatabase.VERSION,
    packageName = "net.simonvt.schematic.sample.provider")
public final class NotesDatabase {

  private NotesDatabase() {
  }

  public static final int VERSION = 1;

  public static class Tables {

    @Table(ListColumns.class) @IfNotExists public static final String LISTS = "lists";

    @Table(TagColumns.class) @IfNotExists public static final String NOTES_TAGS = "notes_tags";
  }

  @Table(NoteColumns.class) public static final String NOTES = "notes";

  @OnCreate public static void onCreate(Context context, SQLiteDatabase db) {
  }

  @OnUpgrade public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion,
      int newVersion) {
  }

  @OnConfigure public static void onConfigure(SQLiteDatabase db) {
  }

  @ExecOnCreate public static final String EXEC_ON_CREATE = "SELECT * FROM " + NOTES;
}
