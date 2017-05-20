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

/*
 * Modifications:
 * -Imported from AOSP frameworks/base/core/java/com/android/internal/content
 * -Changed package name
 */

package net.simonvt.schematic.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Helper for building selection clauses for {@link SQLiteDatabase}. Each
 * appended clause is combined using {@code AND}. This class is <em>not</em>
 * thread safe.
 */
public class SelectionBuilder {

  private static final String TAG = "SelectionBuilder";
  private static final boolean LOGV = false;

  private String table = null;
  private Map<String, String> projectionMap = new HashMap<>();
  private StringBuilder selection = new StringBuilder();
  private ArrayList<String> selectionArgs = new ArrayList<>();

  /** Reset any internal state, allowing this builder to be recycled. */
  public SelectionBuilder reset() {
    table = null;
    selection.setLength(0);
    selectionArgs.clear();
    return this;
  }

  /**
   * Append the given selection clause to the internal state. Each clause is
   * surrounded with parenthesis and combined using {@code AND}.
   */
  public SelectionBuilder where(String selection, @Nullable String... selectionArgs) {
    if (TextUtils.isEmpty(selection)) {
      if (selectionArgs != null && selectionArgs.length > 0) {
        throw new IllegalArgumentException("Valid selection required when including arguments=");
      }

      // Shortcut when clause is empty
      return this;
    }

    if (this.selection.length() > 0) {
      this.selection.append(" AND ");
    }

    this.selection.append("(").append(selection).append(")");
    if (selectionArgs != null) {
      for (String arg : selectionArgs) {
        this.selectionArgs.add(arg);
      }
    }

    return this;
  }

  public SelectionBuilder table(String table) {
    this.table = table;
    return this;
  }

  private void assertTable() {
    if (table == null) {
      throw new IllegalStateException("Table not specified");
    }
  }

  public SelectionBuilder mapToTable(String column, String table) {
    projectionMap.put(column, table + "." + column);
    return this;
  }

  public SelectionBuilder map(String fromColumn, String toClause) {
    projectionMap.put(fromColumn, toClause + " AS " + fromColumn);
    return this;
  }

  /**
   * Return selection string for current internal state.
   *
   * @see #getSelectionArgs()
   */
  public String getSelection() {
    return selection.toString();
  }

  /**
   * Return selection arguments for current internal state.
   *
   * @see #getSelection()
   */
  public String[] getSelectionArgs() {
    return selectionArgs.toArray(new String[selectionArgs.size()]);
  }

  private void mapColumns(String[] columns) {
    for (int i = 0; i < columns.length; i++) {
      final String target = projectionMap.get(columns[i]);
      if (target != null) {
        columns[i] = target;
      }
    }
  }

  @Override public String toString() {
    return "SelectionBuilder[table="
        + table
        + ", selection="
        + getSelection()
        + ", selectionArgs="
        + Arrays.toString(getSelectionArgs())
        + "]";
  }

  /** Execute query using the current internal state as {@code WHERE} clause. */
  public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
    return query(db, columns, null, null, orderBy, null);
  }

  /** Execute query using the current internal state as {@code WHERE} clause. */
  public Cursor query(SQLiteDatabase db, @Nullable String[] columns, @Nullable String groupBy,
      @Nullable String having, @Nullable String orderBy, @Nullable String limit) {
    assertTable();
    if (columns != null) mapColumns(columns);
    if (LOGV) Log.v(TAG, "query(columns=" + Arrays.toString(columns) + ") " + this);
    return db.query(table, columns, getSelection(), getSelectionArgs(), groupBy, having, orderBy,
        limit);
  }

  /** Execute update using the current internal state as {@code WHERE} clause. */
  public int update(SQLiteDatabase db, ContentValues values) {
    assertTable();
    if (LOGV) Log.v(TAG, "update() " + this);
    return db.update(table, values, getSelection(), getSelectionArgs());
  }

  /** Execute delete using the current internal state as {@code WHERE} clause. */
  public int delete(SQLiteDatabase db) {
    assertTable();
    if (LOGV) Log.v(TAG, "delete() " + this);
    return db.delete(table, getSelection(), getSelectionArgs());
  }
}
