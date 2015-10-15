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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.NoteColumns;

public class ListAdapter extends CursorAdapter {

  public ListAdapter(Context context, Cursor c) {
    super(context, c, 0);
  }

  @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
    View v = LayoutInflater.from(context).inflate(R.layout.row_note, parent, false);
    v.setTag(new ViewHolder(v));
    return v;
  }

  @Override public void bindView(View view, Context context, Cursor cursor) {
    ViewHolder vh = (ViewHolder) view.getTag();
    final String note = cursor.getString(cursor.getColumnIndex(NoteColumns.NOTE));
    vh.note.setText(note);
    final String status = cursor.getString(cursor.getColumnIndex(NoteColumns.STATUS));
    vh.status.setText(status);
  }

  static class ViewHolder {
    @Bind(R.id.note) TextView note;
    @Bind(R.id.statusText) TextView status;

    ViewHolder(View v) {
      ButterKnife.bind(this, v);
    }
  }
}
