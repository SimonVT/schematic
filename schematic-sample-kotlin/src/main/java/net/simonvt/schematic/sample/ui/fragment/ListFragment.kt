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

package net.simonvt.schematic.sample.ui.fragment

import android.app.Activity
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_list.*
import net.simonvt.schematic.sample.database.NoteColumns
import net.simonvt.schematic.sample.database.NotesProvider.Lists
import net.simonvt.schematic.sample.database.NotesProvider.Notes
import net.simonvt.schematic.sample.kotlin.R
import net.simonvt.schematic.sample.ui.adapter.ListAdapter

class ListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

  interface OnNoteSelectedListener {

    fun onAddNote(listId: Long)

    fun onNoteSelected(listId: Long, noteId: Long, note: String, status: String)

    fun onListRemoved()
  }

  private var listId: Long = -1L

  lateinit var listener: OnNoteSelectedListener

  private var adapter: ListAdapter? = null

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    listener = activity as OnNoteSelectedListener
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments
    listId = args.getLong(ARG_ID)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    list.emptyView = empty
    if (adapter != null) {
      list.adapter = adapter
    }

    addNote.setOnClickListener { listener.onAddNote(listId) }
    list.setOnItemClickListener { _, _, position, id ->
      val c = adapter!!.getItem(position) as Cursor
      val note = c.getString(c.getColumnIndex(NoteColumns.NOTE))
      val status = c.getString(c.getColumnIndex(NoteColumns.STATUS))
      listener.onNoteSelected(listId, id, note, status)
    }

    loaderManager.initLoader(LOADER_NOTES, null, this)
  }

  override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
    inflater!!.inflate(R.menu.menu_remove, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item!!.itemId) {
      R.id.remove -> {
        val appContext = activity.applicationContext
        val id = listId
        Thread(Runnable {
          appContext.contentResolver.delete(Notes.fromList(id), null, null)
          appContext.contentResolver.delete(Lists.withId(id), null, null)
        }).start()
        listener!!.onListRemoved()
        return true
      }
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return CursorLoader(activity, Notes.fromList(listId), null, null, null, null)
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    if (adapter == null) {
      adapter = ListAdapter(activity, data)
      list?.adapter = adapter
    } else {
      adapter?.changeCursor(data)
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    adapter?.changeCursor(null)
  }

  companion object {

    private val ARG_ID = "net.simonvt.schematic.samples.ui.fragment.ListFragment.id"

    private val LOADER_NOTES = 20

    fun newInstance(id: Long): ListFragment {
      val f = ListFragment()

      val args = Bundle()
      args.putLong(ARG_ID, id)
      f.arguments = args

      return f
    }
  }
}
