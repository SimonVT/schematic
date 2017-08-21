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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import net.simonvt.schematic.annotation.ContentProvider
import net.simonvt.schematic.annotation.ContentUri
import net.simonvt.schematic.annotation.InexactContentUri
import net.simonvt.schematic.annotation.MapColumns
import net.simonvt.schematic.annotation.NotifyBulkInsert
import net.simonvt.schematic.annotation.NotifyDelete
import net.simonvt.schematic.annotation.NotifyInsert
import net.simonvt.schematic.annotation.NotifyUpdate
import net.simonvt.schematic.annotation.TableEndpoint
import net.simonvt.schematic.sample.database.NotesDatabase.Tables
import net.simonvt.schematic.sample.kotlin.BuildConfig
import java.util.*

@ContentProvider(
    authority = NotesProvider.AUTHORITY,
    database = NotesDatabase::class,
    packageName = "net.simonvt.schematic.sample.provider"
)
object NotesProvider {

  const val AUTHORITY = BuildConfig.PROVIDER_AUTHORITY

  val BASE_CONTENT_URI: Uri = Uri.parse("content://" + AUTHORITY)

  interface Path {
    companion object {
      const val LISTS = "lists"
      const val NOTES = "notes"
      const val FROM_LIST = "fromList"
      const val TAGS = "tags"
    }
  }

  private fun buildUri(vararg paths: String): Uri {
    val builder = BASE_CONTENT_URI.buildUpon()
    for (path in paths) {
      builder.appendPath(path)
    }
    return builder.build()
  }

  @TableEndpoint(table = Tables.LISTS)
  object Lists {

    @MapColumns
    @JvmStatic
    fun mapColumns(): Map<String, String> {
      val map = HashMap<String, String>()

      map.put(ListColumns.NOTES, LIST_COUNT)

      return map
    }

    @ContentUri(
        path = Path.LISTS,
        type = "vnd.android.cursor.dir/list",
        defaultSort = ListColumns.TITLE + " ASC"
    )
    val CONTENT_URI = buildUri(Path.LISTS)

    @InexactContentUri(
        path = Path.LISTS + "/#",
        name = "LIST_ID",
        type = "vnd.android.cursor.item/list",
        whereColumn = arrayOf(ListColumns.ID),
        pathSegment = intArrayOf(1)
    )
    @JvmStatic
    fun withId(id: Long): Uri {
      return buildUri(Path.LISTS, id.toString())
    }

    const val LIST_COUNT = "(SELECT COUNT(*) FROM " + NotesDatabase.NOTES + " WHERE " + NotesDatabase.NOTES + "." + NoteColumns.LIST_ID + "=" + Tables.LISTS + "." + ListColumns.ID + ")"
  }

  @TableEndpoint(table = NotesDatabase.NOTES)
  object Notes {

    @ContentUri(path = Path.NOTES, type = "vnd.android.cursor.dir/note")
    val CONTENT_URI = buildUri(Path.NOTES)

    @InexactContentUri(
        name = "NOTE_ID",
        path = Path.NOTES + "/#",
        type = "vnd.android.cursor.item/note",
        whereColumn = arrayOf(NoteColumns.ID),
        pathSegment = intArrayOf(1)
    )
    fun withId(id: Long): Uri {
      return buildUri(Path.NOTES, id.toString())
    }

    @InexactContentUri(
        name = "NOTES_FROM_LIST",
        path = Path.NOTES + "/" + Path.FROM_LIST + "/#",
        type = "vnd.android.cursor.dir/list",
        whereColumn = arrayOf(NoteColumns.LIST_ID),
        pathSegment = intArrayOf(2)
    )
    fun fromList(listId: Long): Uri {
      return buildUri(Path.NOTES, Path.FROM_LIST, listId.toString())
    }

    @NotifyInsert(paths = arrayOf(Path.NOTES))
    @JvmStatic
    fun onInsert(values: ContentValues): Array<Uri> {
      val listId = values.getAsLong(NoteColumns.LIST_ID)!!
      return arrayOf(Lists.withId(listId), fromList(listId))
    }

    @NotifyBulkInsert(paths = arrayOf(Path.NOTES))
    @JvmStatic
    fun onBulkInsert(context: Context, uri: Uri, values: Array<ContentValues>,
        ids: LongArray): Array<Uri> {
      return arrayOf(uri)
    }

    @NotifyUpdate(paths = arrayOf(Path.NOTES + "/#"))
    @JvmStatic
    fun onUpdate(context: Context, uri: Uri, where: String, whereArgs: Array<String>): Array<Uri> {
      val noteId = java.lang.Long.valueOf(uri.pathSegments[1])!!
      val c = context.contentResolver.query(uri, arrayOf(NoteColumns.LIST_ID), null, null, null)
      c!!.moveToFirst()
      val listId = c.getLong(c.getColumnIndex(NoteColumns.LIST_ID))
      c.close()

      return arrayOf(withId(noteId), fromList(listId), Lists.withId(listId))
    }

    @NotifyDelete(paths = arrayOf(Path.NOTES + "/#"))
    @JvmStatic
    fun onDelete(context: Context, uri: Uri): Array<Uri> {
      val noteId = java.lang.Long.valueOf(uri.pathSegments[1])!!
      val c = context.contentResolver.query(uri, null, null, null, null)
      c!!.moveToFirst()
      val listId = c.getLong(c.getColumnIndex(NoteColumns.LIST_ID))
      c.close()

      return arrayOf(withId(noteId), fromList(listId), Lists.withId(listId))
    }
  }

  @TableEndpoint(table = Tables.NOTES_TAGS)
  object NotesTags {

    @InexactContentUri(
        name = "TAGS_FOR_NOTE",
        path = Path.NOTES + "/#/" + Path.TAGS,
        type = "vnd.android.cursor.dir/note/tags",
        whereColumn = arrayOf(TagColumns.NOTE_ID),
        pathSegment = intArrayOf(1)
    )
    fun fromNote(noteId: Long): Uri {
      return buildUri(Path.NOTES, noteId.toString(), Path.TAGS)
    }
  }
}
