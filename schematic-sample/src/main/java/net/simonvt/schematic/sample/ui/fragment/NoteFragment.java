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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import net.simonvt.schematic.Cursors;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.NoteColumns;
import net.simonvt.schematic.sample.database.NotesProvider;
import net.simonvt.schematic.sample.database.NotesProvider.Notes;
import net.simonvt.schematic.sample.database.TagColumns;
import net.simonvt.schematic.sample.provider.values.Notes_tagsValuesBuilder;

public class NoteFragment extends Fragment {

  public interface NoteListener {

    void onNoteChange();

    void onNoteRemoved();
  }

  private static final String ARG_LIST_ID =
      "net.simonvt.schematic.samples.ui.fragment.NoteFragment.listId";
  private static final String ARG_ID = "net.simonvt.schematic.samples.ui.fragment.NoteFragment.id";
  private static final String ARG_NOTE =
      "net.simonvt.schematic.samples.ui.fragment.NoteFragment.note";
  private static final String ARG_STATUS =
      "net.simonvt.schematic.samples.ui.fragment.NoteFragment.status";

  private static final long NO_ID = -1L;

  public static NoteFragment newInstance(long listId) {
    return newInstance(listId, NO_ID, null, null);
  }

  public static NoteFragment newInstance(long listId, long id, String note, String status) {
    NoteFragment f = new NoteFragment();

    Bundle args = new Bundle();
    args.putLong(ARG_LIST_ID, listId);
    args.putLong(ARG_ID, id);
    args.putString(ARG_NOTE, note);
    args.putString(ARG_STATUS, status);
    f.setArguments(args);

    return f;
  }

  private long listId;
  private long noteId;
  private String note;
  private String status;

  private NoteListener listener;

  Unbinder unbinder;

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.action) View actionView;
  @BindView(R.id.actionText) TextView actionText;
  @BindView(R.id.noteInput) EditText noteView;
  @BindView(R.id.statusSwitch) Switch statusView;
  @BindView(R.id.tags) EditText tags;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (NoteListener) activity;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle args = getArguments();
    listId = args.getLong(ARG_LIST_ID);
    noteId = args.getLong(ARG_ID);
    note = args.getString(ARG_NOTE);
    status = args.getString(ARG_STATUS);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_note, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);
    LoaderManager.LoaderCallbacks<Cursor> callBack = new LoaderManager.LoaderCallbacks<Cursor>() {
      @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), NotesProvider.NotesTags.fromNote(noteId), null, null,
            null, null);
      }

      @Override public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        data.moveToPosition(-1);
        StringBuffer buffer = new StringBuffer();
        while (data.moveToNext()) {
          if (buffer.length() > 0) {
            buffer.append(",");
          }
          buffer.append(Cursors.getString(data, TagColumns.NAME));
        }
        tags.setText(buffer.toString());
      }

      @Override public void onLoaderReset(Loader<Cursor> loader) {

      }
    };
    if (noteId != NO_ID) {
      noteView.setText(note);
      statusView.setChecked(NoteColumns.STATUS_COMPLETED.equals(status));
      actionText.setText(R.string.update);
      getLoaderManager().initLoader(1, null, callBack);
    } else {
      statusView.setChecked(false);
      actionText.setText(R.string.insert);
    }

    toolbar.setTitle(R.string.app_name);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        getActivity().onBackPressed();
      }
    });
    toolbar.inflateMenu(R.menu.menu_remove);
    toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem item) {
        return onMenuItemSelected(item);
      }
    });
  }

  @Override public void onDestroyView() {
    if (unbinder != null) {
      unbinder.unbind();
      unbinder = null;
    }
    super.onDestroyView();
  }

  public boolean onMenuItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.remove:
        final Context appContext = getActivity().getApplicationContext();
        final long id = noteId;
        new Thread(new Runnable() {
          @Override public void run() {
            appContext.getContentResolver().delete(Notes.withId(id), null, null);
          }
        }).start();
        listener.onNoteRemoved();
        return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @OnClick(R.id.action) void onAction() {
    final String note = noteView.getText().toString();
    final String status;
    if (statusView.isChecked()) {
      status = NoteColumns.STATUS_COMPLETED;
    } else {
      status = NoteColumns.STATUS_NEW;
    }
    final String tagList = tags.getText().toString();
    final Context appContext = getActivity().getApplicationContext();
    if (noteId == NO_ID) {
      new Thread(new Runnable() {
        @Override public void run() {
          ContentValues cv = new ContentValues();
          cv.put(NoteColumns.LIST_ID, listId);
          cv.put(NoteColumns.NOTE, note);
          cv.put(NoteColumns.STATUS, status);
          Uri newUri = appContext.getContentResolver().insert(Notes.CONTENT_URI, cv);
          long newId = Long.parseLong(newUri.getLastPathSegment());
          insertTags(newId, tagList, appContext);
        }
      }).start();
    } else {
      final long id = noteId;
      new Thread(new Runnable() {
        @Override public void run() {
          ContentValues cv = new ContentValues();
          cv.put(NoteColumns.NOTE, note);
          cv.put(NoteColumns.STATUS, status);
          appContext.getContentResolver().update(Notes.withId(noteId), cv, null, null);
          appContext.getContentResolver()
              .delete(NotesProvider.NotesTags.fromNote(noteId), null, null);
          insertTags(id, tagList, appContext);
        }
      }).start();
    }

    listener.onNoteChange();
  }

  private static void insertTags(long newId, String tagList, Context appContext) {
    String[] tagNames = tagList.split(",");
    ContentValues[] tagValues = new ContentValues[tagNames.length];
    for (int i = 0; i < tagNames.length; i++) {
      tagValues[i] = new Notes_tagsValuesBuilder().noteId(newId).name(tagNames[i]).values();
    }
    appContext.getContentResolver().bulkInsert(NotesProvider.NotesTags.fromNote(newId), tagValues);
  }
}
