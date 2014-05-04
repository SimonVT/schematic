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

import android.content.ContentValues;
import android.net.Uri;
import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.NotificationUri;
import net.simonvt.schematic.annotation.NotifyDelete;
import net.simonvt.schematic.annotation.NotifyInsert;
import net.simonvt.schematic.annotation.NotifyUpdate;
import net.simonvt.schematic.annotation.TableEndpoint;
import net.simonvt.schematic.annotation.Where;

@ContentProvider(name = "MyProvider", authority = MyProvider.AUTHORITY, database = MyDatabase.class)
public class MyProvider {

  public static final String AUTHORITY = "net.simonvt.schematic.sample.MyProvider";

  static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  @TableEndpoint(table = MyDatabase.DATA) public static class Data {

    @ContentUri(
        path = "data",
        type = "vnd.android.cursor.dir/data",

        where = {
            DataColumns._ID + ">0"
        },

        defaultSort = DataColumns._ID + " ASC")
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("data").build();

    @InexactContentUri(
        name = "DATA_ID",
        path = "data/#",
        type = "vnd.android.cursor.dir/data",
        join = "test",
        whereColumn = DataColumns._ID,
        pathSegment = 1)
    public static Uri withId(long id) {
      return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Where(path = "data/#") public static String[] withIdWhere(Uri uri) {
      return new String[] {
        DataColumns._ID + ">=0",
      };
    }

    @NotifyInsert(paths = "data") public static Uri[] onDataIdInsert(Uri uri, ContentValues cv,
        long id) {
      return new Uri[] {
          withId(id),
      };
    }

    @NotifyUpdate(paths = "data/#")
    public static Uri[] onDataIdUpdate(Uri uri, ContentValues values, String where, String[] whereArgs) {
      return new Uri[] {
          uri,
      };
    }

    @NotifyDelete(paths = "data/#")
    public static Uri[] onDataIdDelete(Uri uri) {
      return new Uri[] {
          uri,
      };
    }

    public static long getId(Uri uri) {
      return Long.valueOf(uri.getPathSegments().get(1));
    }
  }

  @TableEndpoint(table = MyDatabase.MORE_DATA) public static class MoreData {

    @ContentUri(
        path = "more",
        type = "vnd.android.cursor.dir/moreData")
    public static final Uri MORE_DATA = BASE_CONTENT_URI.buildUpon().appendPath("more").build();

    @NotificationUri(paths = "more")
    public static final Uri MORE_DATA_NOTIFICATION =
        BASE_CONTENT_URI.buildUpon().appendPath("moreNotification").build();
  }
}
