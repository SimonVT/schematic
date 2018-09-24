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
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.simonvt.schematic.Cursors
import net.simonvt.schematic.sample.database.ListColumns
import net.simonvt.schematic.sample.kotlin.R
import net.simonvt.schematic.sample.ui.fragment.ListsFragment
import net.simonvt.schematic.sample.util.inflate
import net.torrenttoise.util.bindView
import net.torrenttoise.util.getLong

class ListsAdapter(private val context: Context,
    private val listener: ListsFragment.OnListSelectedListener) :
    RecyclerView.Adapter<ListsAdapter.ViewHolder>() {

  private var _cursor: Cursor? = null
  var cursor: Cursor?
    get() = _cursor
    set(value) {
      _cursor = value
      notifyDataSetChanged()
    }

  init {
    setHasStableIds(true)
  }

  override fun getItemCount(): Int = cursor?.count ?: 0

  override fun getItemId(position: Int): Long {
    cursor!!.moveToPosition(position)
    return cursor!!.getLong(ListColumns.ID)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = parent.inflate(R.layout.row_list)
    val holder = ViewHolder(view)
    view.setOnClickListener {
      if (holder.adapterPosition != RecyclerView.NO_POSITION) {
        listener.onListSelected(holder.itemId)
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    cursor?.moveToPosition(position)
    val title = Cursors.getString(cursor, ListColumns.TITLE)
    holder.title.text = title
    val notes = Cursors.getInt(cursor, ListColumns.NOTES)
    holder.notes.text = context.getString(R.string.x_notes, notes)
  }

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val title by bindView<TextView>(R.id.title)
    val notes by bindView<TextView>(R.id.notes)
  }

  companion object {

    val PROJECTION = arrayOf(ListColumns.ID, ListColumns.TITLE, ListColumns.NOTES)
  }
}
