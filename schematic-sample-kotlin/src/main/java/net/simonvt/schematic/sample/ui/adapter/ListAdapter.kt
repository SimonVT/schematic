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

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import net.simonvt.schematic.Cursors
import net.simonvt.schematic.sample.database.ListColumns
import net.simonvt.schematic.sample.database.NoteColumns
import net.simonvt.schematic.sample.kotlin.R
import net.simonvt.schematic.sample.ui.fragment.ListFragment
import net.simonvt.schematic.sample.util.inflate
import net.torrenttoise.util.bindView
import net.torrenttoise.util.getLong
import net.torrenttoise.util.getString

class ListAdapter(private val listId: Long,
    private val listener: ListFragment.OnNoteSelectedListener) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {

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
    val view = parent.inflate(R.layout.row_note)
    val holder = ViewHolder(view)
    view.setOnClickListener {
      if (holder.adapterPosition != RecyclerView.NO_POSITION) {
        cursor!!.moveToPosition(holder.adapterPosition)
        val note = cursor!!.getString(NoteColumns.NOTE)
        val status = cursor!!.getString(NoteColumns.STATUS)
        listener.onNoteSelected(listId, holder.itemId, note, status)
      }
    }
    return holder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    cursor?.moveToPosition(position)
    val note = Cursors.getString(cursor, NoteColumns.NOTE)
    holder.note.text = note
    val status = Cursors.getString(cursor, NoteColumns.STATUS)
    holder.status.text = status
  }

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val note by bindView<TextView>(R.id.note)
    val status by bindView<TextView>(R.id.statusText)
  }
}
