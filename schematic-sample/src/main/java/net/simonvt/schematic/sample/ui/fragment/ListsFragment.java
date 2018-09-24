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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
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

  Unbinder unbinder;

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(android.R.id.list) RecyclerView list;
  @BindView(android.R.id.empty) TextView emptyView;

  private ListsAdapter adapter;

  OnListSelectedListener listener;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (OnListSelectedListener) activity;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adapter = new ListsAdapter(getContext(), listener);
    adapter.registerAdapterDataObserver(adapterObserver);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_lists, container, false);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = ButterKnife.bind(this, view);
    list.setAdapter(adapter);
    list.setLayoutManager(new LinearLayoutManager(getContext()));
    showEmptyView(adapter.getItemCount() == 0);

    toolbar.setTitle(R.string.app_name);

    getLoaderManager().initLoader(LOADER_LISTS, null, this);
  }

  @Override public void onDestroyView() {
    if (unbinder != null) {
      unbinder.unbind();
      unbinder = null;
    }
    super.onDestroyView();
  }

  private void showEmptyView(boolean show) {
    if (emptyView != null) {
      if (show) {
        emptyView.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
      } else {
        emptyView.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
      }
    }
  }

  private RecyclerView.AdapterDataObserver adapterObserver =
      new RecyclerView.AdapterDataObserver() {

        @Override public void onChanged() {
          int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeChanged(int positionStart, int itemCount) {
          int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
          int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
          int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }
      };

  @OnClick(R.id.addList) void addList() {
    new NewListDialog().show(getFragmentManager(), DIALOG_NEW_LIST);
  }

  @Override public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(getActivity(), Lists.CONTENT_URI, ListsAdapter.PROJECTION, null, null,
        null);
  }

  @Override public void onLoadFinished(Loader loader, Cursor data) {
    adapter.setCursor(data);
  }

  @Override public void onLoaderReset(Loader loader) {
    adapter.setCursor(null);
  }
}
