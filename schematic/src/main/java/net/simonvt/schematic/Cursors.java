/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.schematic;

import android.database.Cursor;
import javax.annotation.Nullable;

public final class Cursors {

  private Cursors() {
  }

  public static @Nullable String getString(Cursor cursor, String column) {
    return cursor.getString(cursor.getColumnIndexOrThrow(column));
  }

  public static @Nullable String getStringOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getString(index);
  }

  public static int getInt(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndexOrThrow(column));
  }

  public static @Nullable Integer getIntOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getInt(index);
  }

  public static long getLong(Cursor cursor, String column) {
    return cursor.getLong(cursor.getColumnIndexOrThrow(column));
  }

  public static @Nullable Long getLongOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getLong(index);
  }

  public static float getFloat(Cursor cursor, String column) {
    return cursor.getFloat(cursor.getColumnIndexOrThrow(column));
  }

  public static @Nullable Float getFloatOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getFloat(index);
  }

  public static double getDouble(Cursor cursor, String column) {
    return cursor.getDouble(cursor.getColumnIndexOrThrow(column));
  }

  public static @Nullable Double getDoubleOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getDouble(index);
  }

  public static boolean getBoolean(Cursor cursor, String column) {
    return cursor.getInt(cursor.getColumnIndexOrThrow(column)) == 1;
  }

  public static Boolean getBooleanOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getInt(index) == 1;
  }

  public static byte[] getBlob(Cursor cursor, String column) {
    return cursor.getBlob(cursor.getColumnIndexOrThrow(column));
  }

  public static byte[] getBlobOrNull(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    if (cursor.isNull(index)) {
      return null;
    }

    return cursor.getBlob(index);
  }
}
