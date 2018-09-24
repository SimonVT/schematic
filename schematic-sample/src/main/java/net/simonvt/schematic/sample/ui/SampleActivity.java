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

package net.simonvt.schematic.sample.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import net.simonvt.schematic.sample.ui.fragment.ListFragment;
import net.simonvt.schematic.sample.ui.fragment.ListsFragment;
import net.simonvt.schematic.sample.ui.fragment.NoteFragment;

public class SampleActivity extends FragmentActivity
    implements ListsFragment.OnListSelectedListener, ListFragment.OnNoteSelectedListener,
    NoteFragment.NoteListener {

  private static final String FRAGMENT_LISTS =
      "net.simonvt.schematic.samples.ui.SampleActivity.LISTS";
  private static final String FRAGMENT_LIST =
      "net.simonvt.schematic.samples.ui.SampleActivity.LIST";
  private static final String FRAGMENT_NOTE =
      "net.simonvt.schematic.samples.ui.SampleActivity.NOTE";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      Fragment f = new ListsFragment();
      getSupportFragmentManager() //
          .beginTransaction() //
          .add(android.R.id.content, f, FRAGMENT_LISTS) //
          .commit();
    }
  }

  @Override public void onListSelected(long listId) {
    Fragment f = ListFragment.newInstance(listId);
    getSupportFragmentManager() //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_LIST) //
        .addToBackStack(null) //
        .commit();
  }

  @Override public void onAddNote(long listId) {
    Fragment f = NoteFragment.newInstance(listId);
    getSupportFragmentManager() //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_NOTE) //
        .addToBackStack(null) //
        .commit();
  }

  @Override public void onNoteSelected(long listId, long noteId, String note, String status) {
    Fragment f = NoteFragment.newInstance(listId, noteId, note, status);
    getSupportFragmentManager() //
        .beginTransaction() //
        .replace(android.R.id.content, f, FRAGMENT_NOTE) //
        .addToBackStack(null) //
        .commit();
  }

  @Override public void onListRemoved() {
    getSupportFragmentManager().popBackStack();
  }

  @Override public void onNoteChange() {
    getSupportFragmentManager().popBackStack();
  }

  @Override public void onNoteRemoved() {
    getSupportFragmentManager().popBackStack();
  }
}
