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

package net.simonvt.schematic.sample.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.schematic.Cursors;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.ListColumns;
import net.simonvt.schematic.sample.ui.fragment.ListsFragment;

public class ListsAdapter extends RecyclerView.Adapter<ListsAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      ListColumns.ID, ListColumns.TITLE, ListColumns.NOTES,
  };

  private Context context;
  private ListsFragment.OnListSelectedListener listener;

  private Cursor cursor;

  public ListsAdapter(Context context, ListsFragment.OnListSelectedListener listener) {
    this.context = context;
    this.listener = listener;
    setHasStableIds(true);
  }

  public void setCursor(Cursor cursor) {
    this.cursor = cursor;
    notifyDataSetChanged();
  }

  @Override public int getItemCount() {
    if (cursor != null) {
      return cursor.getCount();
    }

    return 0;
  }

  @Override public long getItemId(int position) {
    cursor.moveToPosition(position);
    return Cursors.getLong(cursor, ListColumns.ID);
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_list, parent, false);
    final ViewHolder holder = new ViewHolder(view);
    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
          listener.onListSelected(holder.getItemId());
        }
      }
    });
    return holder;
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    cursor.moveToPosition(position);
    final String title = Cursors.getString(cursor, ListColumns.TITLE);
    holder.title.setText(title);
    final int notes = Cursors.getInt(cursor, ListColumns.NOTES);
    holder.notes.setText(context.getString(R.string.x_notes, notes));
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.title) TextView title;
    @BindView(R.id.notes) TextView notes;

    ViewHolder(View view) {
      super(view);
      ButterKnife.bind(this, view);
    }
  }
}
