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
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_lists.*
import net.simonvt.schematic.sample.database.NotesProvider.Lists
import net.simonvt.schematic.sample.kotlin.R
import net.simonvt.schematic.sample.ui.adapter.ListsAdapter
import net.simonvt.schematic.sample.ui.dialog.NewListDialog

class ListsFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

  interface OnListSelectedListener {

    fun onListSelected(listId: Long)
  }

  private var adapter: ListsAdapter? = null

  lateinit var listener: OnListSelectedListener

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    listener = activity as OnListSelectedListener
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_lists, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    list.emptyView = empty
    if (adapter != null) {
      list.adapter = adapter
    }

    addList.setOnClickListener { NewListDialog().show(fragmentManager, DIALOG_NEW_LIST) }
    list.setOnItemClickListener { _, _, _, id -> listener.onListSelected(id) }

    loaderManager.initLoader(LOADER_LISTS, null, this)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
    return CursorLoader(activity, Lists.CONTENT_URI, ListsAdapter.PROJECTION, null, null, null)
  }

  override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
    if (adapter == null) {
      adapter = ListsAdapter(activity, data)
      list?.adapter = adapter
    } else {
      adapter!!.changeCursor(data)
    }
  }

  override fun onLoaderReset(loader: Loader<Cursor>) {
    adapter?.changeCursor(null)
  }

  companion object {

    private val LOADER_LISTS = 10

    private val DIALOG_NEW_LIST = "net.simonvt.schematic.samples.ui.fragment.ListsFragment.newList"
  }
}
