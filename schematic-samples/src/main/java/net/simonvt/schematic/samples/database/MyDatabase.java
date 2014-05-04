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

package net.simonvt.schematic.samples.database;

import android.database.sqlite.SQLiteDatabase;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;
import net.simonvt.schematic.annotation.Version;

@Database(className = "MyDatabase", fileName = "myDatabase.db")
public class MyDatabase {

  @Version public static final int VERSION = 2;

  @Table(DataColumns.class) public static final String DATA = "data";

  @Table(MoreData.class) public static final String MORE_DATA = "moreData";

  @OnUpgrade public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

  }

  @ExecOnCreate public static final String ON_CREATE = "SELECT * FROM " + DATA;
}
