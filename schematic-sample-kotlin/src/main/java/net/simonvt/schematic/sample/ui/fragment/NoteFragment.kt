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
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_note.*
import kotlinx.android.synthetic.main.toolbar.*
import net.simonvt.schematic.Cursors
import net.simonvt.schematic.sample.database.NoteColumns
import net.simonvt.schematic.sample.database.NotesProvider
import net.simonvt.schematic.sample.database.NotesProvider.Notes
import net.simonvt.schematic.sample.database.TagColumns
import net.simonvt.schematic.sample.kotlin.R
import net.simonvt.schematic.sample.provider.values.Notes_tagsValuesBuilder

class NoteFragment : Fragment() {

  interface NoteListener {

    fun onNoteChange()

    fun onNoteRemoved()
  }

  private var listId: Long = 0
  private var noteId: Long = 0
  private var note: String? = null
  private var status: String? = null

  lateinit var listener: NoteListener

  override fun onAttach(activity: Activity?) {
    super.onAttach(activity)
    listener = activity as NoteListener
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = arguments
    listId = args.getLong(ARG_LIST_ID)
    noteId = args.getLong(ARG_ID)
    note = args.getString(ARG_NOTE)
    status = args.getString(ARG_STATUS)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_note, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    action.setOnClickListener { onActionClicked() }
    if (noteId != NO_ID) {
      noteInput.setText(note)
      statusSwitch.isChecked = NoteColumns.STATUS_COMPLETED == status
      actionText.setText(R.string.update)
      loaderManager.initLoader(1, null, callBack)
    } else {
      statusSwitch.isChecked = false
      actionText.setText(R.string.insert)
    }

    toolbar.setTitle(R.string.app_name)
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
    toolbar.setNavigationOnClickListener { activity.onBackPressed() }
    toolbar.inflateMenu(R.menu.menu_remove)
    toolbar.setOnMenuItemClickListener { item -> onMenuItemSelected(item) }
  }

  private fun onMenuItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.remove -> {
        val appContext = activity.applicationContext
        val id = noteId
        Thread(Runnable { appContext.contentResolver.delete(Notes.withId(id), null, null) }).start()
        listener.onNoteRemoved()
        return true
      }
      else -> return false
    }
  }

  private fun onActionClicked() {
    val note = noteInput.text.toString()
    val status: String
    if (statusSwitch.isChecked) {
      status = NoteColumns.STATUS_COMPLETED
    } else {
      status = NoteColumns.STATUS_NEW
    }
    val tagList = tags!!.text.toString()
    val appContext = activity.applicationContext
    if (noteId == NO_ID) {
      Thread(Runnable {
        val cv = ContentValues()
        cv.put(NoteColumns.LIST_ID, listId)
        cv.put(NoteColumns.NOTE, note)
        cv.put(NoteColumns.STATUS, status)
        val newUri = appContext.contentResolver.insert(Notes.CONTENT_URI, cv)
        val newId = java.lang.Long.parseLong(newUri!!.lastPathSegment)
        insertTags(newId, tagList, appContext)
      }).start()
    } else {
      val id = noteId
      Thread(Runnable {
        val cv = ContentValues()
        cv.put(NoteColumns.NOTE, note)
        cv.put(NoteColumns.STATUS, status)
        appContext.contentResolver.update(Notes.withId(noteId), cv, null, null)
        appContext.contentResolver.delete(NotesProvider.NotesTags.fromNote(noteId), null, null)
        insertTags(id, tagList, appContext)
      }).start()
    }

    listener.onNoteChange()
  }

  val callBack = object : LoaderManager.LoaderCallbacks<Cursor> {
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
      return CursorLoader(activity, NotesProvider.NotesTags.fromNote(noteId), null, null, null,
          null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
      data.moveToPosition(-1)
      val buffer = StringBuffer()
      while (data.moveToNext()) {
        if (buffer.isNotEmpty()) {
          buffer.append(",")
        }
        buffer.append(Cursors.getString(data, TagColumns.NAME))
      }
      tags.setText(buffer.toString())
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }
  }

  companion object {

    private val ARG_LIST_ID = "net.simonvt.schematic.samples.ui.fragment.NoteFragment.listId"
    private val ARG_ID = "net.simonvt.schematic.samples.ui.fragment.NoteFragment.id"
    private val ARG_NOTE = "net.simonvt.schematic.samples.ui.fragment.NoteFragment.note"
    private val ARG_STATUS = "net.simonvt.schematic.samples.ui.fragment.NoteFragment.status"

    private val NO_ID = -1L

    @JvmOverloads
    fun newInstance(listId: Long, id: Long = NO_ID, note: String? = null,
        status: String? = null): NoteFragment {
      val f = NoteFragment()

      val args = Bundle()
      args.putLong(ARG_LIST_ID, listId)
      args.putLong(ARG_ID, id)
      args.putString(ARG_NOTE, note)
      args.putString(ARG_STATUS, status)
      f.arguments = args

      return f
    }

    private fun insertTags(newId: Long, tagList: String, appContext: Context) {
      val tagNames = tagList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val tagValues = arrayOfNulls<ContentValues>(tagNames.size)
      for (i in tagNames.indices) {
        tagValues[i] = Notes_tagsValuesBuilder()
            .noteId(newId)
            .name(tagNames[i])
            .values()
      }
      appContext.contentResolver
          .bulkInsert(NotesProvider.NotesTags.fromNote(newId), tagValues)
    }
  }
}
