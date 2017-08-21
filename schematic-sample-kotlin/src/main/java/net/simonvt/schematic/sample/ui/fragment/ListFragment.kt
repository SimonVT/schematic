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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.toolbar.*
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

  lateinit var adapter: ListAdapter

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    listener = activity as OnNoteSelectedListener
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments
    listId = args.getLong(ARG_ID)
    adapter = ListAdapter(listId, listener)
    adapter.registerAdapterDataObserver(adapterObserver)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_list, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    list.adapter = adapter
    list.layoutManager = LinearLayoutManager(context)
    showEmptyView(adapter.itemCount == 0)

    addNote.setOnClickListener { listener.onAddNote(listId) }

    toolbar.setTitle(R.string.app_name)
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
    toolbar.setNavigationOnClickListener { activity.onBackPressed() }
    toolbar.inflateMenu(R.menu.menu_remove)
    toolbar.setOnMenuItemClickListener { item -> onMenuItemSelected(item) }

    loaderManager.initLoader(LOADER_NOTES, null, this)
  }

  private fun showEmptyView(show: Boolean) {
    if (show) {
      empty?.visibility = View.VISIBLE
      list?.visibility = View.GONE
    } else {
      empty?.visibility = View.GONE
      list?.visibility = View.VISIBLE
    }
  }

  private val adapterObserver = object : RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
      val adapterItemCount = adapter.itemCount
      showEmptyView(adapterItemCount == 0)
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
      val adapterItemCount = adapter.itemCount
      showEmptyView(adapterItemCount == 0)
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
      val adapterItemCount = adapter.itemCount
      showEmptyView(adapterItemCount == 0)
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
      val adapterItemCount = adapter.itemCount
      showEmptyView(adapterItemCount == 0)
    }
  }

  private fun onMenuItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.remove -> {
        val appContext = activity.applicationContext
        val id = listId
        Thread(Runnable {
          appContext.contentResolver.delete(Notes.fromList(id), null, null)
          appContext.contentResolver.delete(Lists.withId(id), null, null)
        }).start()
        listener.onListRemoved()
        return true
      }
      else -> return false
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return CursorLoader(activity, Notes.fromList(listId), null, null, null, null)
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    adapter.cursor = data
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    adapter.cursor = null
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
