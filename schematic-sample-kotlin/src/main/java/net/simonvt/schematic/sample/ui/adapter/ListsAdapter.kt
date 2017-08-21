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

package net.simonvt.schematic.sample.ui.adapter

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import net.simonvt.schematic.Cursors
import net.simonvt.schematic.sample.database.ListColumns
import net.simonvt.schematic.sample.kotlin.R

class ListsAdapter(context: Context, c: Cursor) : CursorAdapter(context, c, 0) {

  override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
    val v = LayoutInflater.from(context).inflate(R.layout.row_list, parent, false)
    v.tag = ViewHolder(v)
    return v
  }

  override fun bindView(view: View, context: Context, cursor: Cursor) {
    val vh = view.tag as ViewHolder
    val title = Cursors.getString(cursor, ListColumns.TITLE)
    vh.title.text = title
    val notes = Cursors.getInt(cursor, ListColumns.NOTES)
    vh.notes.text = context.getString(R.string.x_notes, notes)
  }

  class ViewHolder(view: View) {

    val title: TextView = view.findViewById(R.id.title)
    val notes: TextView = view.findViewById(R.id.notes)
  }

  companion object {

    val PROJECTION = arrayOf(ListColumns.ID, ListColumns.TITLE, ListColumns.NOTES)
  }
}
