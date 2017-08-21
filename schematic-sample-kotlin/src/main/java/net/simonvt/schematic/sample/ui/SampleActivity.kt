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

package net.simonvt.schematic.sample.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import net.simonvt.schematic.sample.ui.fragment.ListFragment
import net.simonvt.schematic.sample.ui.fragment.ListsFragment
import net.simonvt.schematic.sample.ui.fragment.NoteFragment

class SampleActivity : AppCompatActivity(), ListsFragment.OnListSelectedListener,
    ListFragment.OnNoteSelectedListener, NoteFragment.NoteListener {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      val f = ListsFragment()
      supportFragmentManager //
          .beginTransaction() //
          .add(android.R.id.content, f, FRAGMENT_LISTS) //
          .commit()
    }
  }

  override fun onListSelected(listId: Long) {
    val f = ListFragment.newInstance(listId)
    supportFragmentManager //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_LIST) //
        .addToBackStack(null) //
        .commit()
  }

  override fun onAddNote(listId: Long) {
    val f = NoteFragment.newInstance(listId)
    supportFragmentManager //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_NOTE) //
        .addToBackStack(null) //
        .commit()
  }

  override fun onNoteSelected(listId: Long, noteId: Long, note: String, status: String) {
    val f = NoteFragment.newInstance(listId, noteId, note, status)
    supportFragmentManager //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_NOTE) //
        .addToBackStack(null) //
        .commit()
  }

  override fun onListRemoved() {
    supportFragmentManager.popBackStack()
  }

  override fun onNoteChange() {
    supportFragmentManager.popBackStack()
  }

  override fun onNoteRemoved() {
    supportFragmentManager.popBackStack()
  }

  companion object {

    private val FRAGMENT_LISTS = "net.simonvt.schematic.samples.ui.SampleActivity.LISTS"
    private val FRAGMENT_LIST = "net.simonvt.schematic.samples.ui.SampleActivity.LIST"
    private val FRAGMENT_NOTE = "net.simonvt.schematic.samples.ui.SampleActivity.NOTE"
  }
}
