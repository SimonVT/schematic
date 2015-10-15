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

package net.simonvt.schematic.sample.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.NoteColumns;
import net.simonvt.schematic.sample.database.NotesProvider.Lists;
import net.simonvt.schematic.sample.database.NotesProvider.Notes;
import net.simonvt.schematic.sample.ui.adapter.ListAdapter;

public class ListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  public interface OnNoteSelectedListener {

    void onAddNote(long listId);

    void onNoteSelected(long listId, long noteId, String note, String status);

    void onListRemoved();
  }

  private static final String ARG_ID = "net.simonvt.schematic.samples.ui.fragment.ListFragment.id";

  private static final int LOADER_NOTES = 20;

  public static ListFragment newInstance(long id) {
    ListFragment f = new ListFragment();

    Bundle args = new Bundle();
    args.putLong(ARG_ID, id);
    f.setArguments(args);

    return f;
  }

  private long listId;

  private OnNoteSelectedListener listener;

  private ListAdapter adapter;

  @Bind(android.R.id.list) ListView listView;

  @Bind(android.R.id.empty) TextView emptyView;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (OnNoteSelectedListener) activity;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();
    listId = args.getLong(ARG_ID);

    setHasOptionsMenu(true);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_list, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);
    listView.setEmptyView(emptyView);
    if (adapter != null) {
      listView.setAdapter(adapter);
    }

    getLoaderManager().initLoader(LOADER_NOTES, null, this);
  }

  @Override public void onDestroyView() {
    ButterKnife.unbind(this);
    super.onDestroyView();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_remove, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.remove:
        final Context appContext = getActivity().getApplicationContext();
        final long id = listId;
        new Thread(new Runnable() {
          @Override public void run() {
            appContext.getContentResolver().delete(Notes.fromList(id), null, null);
            appContext.getContentResolver().delete(Lists.withId(id), null, null);
          }
        }).start();
        listener.onListRemoved();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @OnClick(R.id.addNote) void onAddNote() {
    listener.onAddNote(listId);
  }

  @OnItemClick(android.R.id.list) void onNoteSelected(int position, long noteId) {
    Cursor c = (Cursor) adapter.getItem(position);
    String note = c.getString(c.getColumnIndex(NoteColumns.NOTE));
    String status = c.getString(c.getColumnIndex(NoteColumns.STATUS));
    listener.onNoteSelected(listId, noteId, note, status);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(), Notes.fromList(listId), null, null, null, null);
  }

  @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    if (adapter == null) {
      adapter = new ListAdapter(getActivity(), data);
      listView.setAdapter(adapter);
    } else {
      adapter.changeCursor(data);
    }
  }

  @Override public void onLoaderReset(Loader<Cursor> loader) {
    if (adapter != null) {
      adapter.changeCursor(null);
    }
  }
}
