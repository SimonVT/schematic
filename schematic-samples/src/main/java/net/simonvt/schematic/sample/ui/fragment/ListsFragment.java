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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.NotesProvider.Lists;
import net.simonvt.schematic.sample.ui.adapter.ListsAdapter;
import net.simonvt.schematic.sample.ui.dialog.NewListDialog;

public class ListsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  public interface OnListSelectedListener {

    void onListSelected(long listId);
  }

  private static final int LOADER_LISTS = 10;

  private static final String DIALOG_NEW_LIST =
      "net.simonvt.schematic.samples.ui.fragment.ListsFragment.newList";

  @Bind(android.R.id.list) ListView listView;
  @Bind(android.R.id.empty) TextView emptyView;

  private ListsAdapter adapter;

  OnListSelectedListener listener;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (OnListSelectedListener) activity;
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_lists, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);
    listView.setEmptyView(emptyView);
    if (adapter != null) {
      listView.setAdapter(adapter);
    }

    getLoaderManager().initLoader(LOADER_LISTS, null, this);
  }

  @Override public void onDestroyView() {
    ButterKnife.unbind(this);
    super.onDestroyView();
  }

  @OnClick(R.id.addList) void addList() {
    new NewListDialog().show(getFragmentManager(), DIALOG_NEW_LIST);
  }

  @OnItemClick(android.R.id.list) void onListClicked(long id) {
    listener.onListSelected(id);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(), Lists.CONTENT_URI, ListsAdapter.PROJECTION, null, null, null);
  }

  @Override public void onLoadFinished(Loader loader, Cursor data) {
    if (adapter == null) {
      adapter = new ListsAdapter(getActivity(), data);
      listView.setAdapter(adapter);
    } else {
      adapter.changeCursor(data);
    }
  }

  @Override public void onLoaderReset(Loader loader) {
    if (adapter != null) {
      adapter.changeCursor(null);
    }
  }
}
