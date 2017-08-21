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

package net.simonvt.schematic.sample.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import net.simonvt.schematic.sample.R;
import net.simonvt.schematic.sample.database.ListColumns;
import net.simonvt.schematic.sample.database.NotesProvider.Lists;

public class NewListDialog extends DialogFragment {

  @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    View v = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_new_list, null);
    final EditText listName = (EditText) v.findViewById(R.id.listName);

    builder.setView(v);

    builder.setTitle(R.string.new_list)
        .setPositiveButton(R.string.create_list, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            final String name = listName.getText().toString();
            final Context context = getActivity().getApplicationContext();
            new Thread(new Runnable() {
              @Override public void run() {
                ContentValues cv = new ContentValues();
                cv.put(ListColumns.TITLE, name);
                context.getContentResolver().insert(Lists.CONTENT_URI, cv);
              }
            }).start();
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
          }
        });

    return builder.create();
  }
}
