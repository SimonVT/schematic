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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.ListColumns;

public class ListsAdapter extends CursorAdapter {

  public static final String[] PROJECTION = new String[] {
      ListColumns.ID, ListColumns.TITLE, ListColumns.NOTES,
  };

  public ListsAdapter(Context context, Cursor c) {
    super(context, c, 0);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.row_list, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();
    final String title = cursor.getString(cursor.getColumnIndex(ListColumns.TITLE));
    vh.title.setText(title);
    final int notes = cursor.getInt(cursor.getColumnIndex(ListColumns.NOTES));
    vh.notes.setText(context.getString(R.string.x_notes, notes));
  }

  static class ViewHolder {

    @Bind(R.id.title) TextView title;
    @Bind(R.id.notes) TextView notes;

    public ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }
}
