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

package net.simonvt.schematic.sample.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.widget.EditText
import net.simonvt.schematic.sample.database.ListColumns
import net.simonvt.schematic.sample.database.NotesProvider.Lists
import net.simonvt.schematic.sample.kotlin.R

class NewListDialog : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)

    val view = LayoutInflater.from(builder.context).inflate(R.layout.dialog_new_list, null)
    val listName = view.findViewById<EditText>(R.id.listName)

    builder.setView(view)
        .setTitle(R.string.new_list)
        .setPositiveButton(R.string.create_list) { _, _ ->
          val name = listName.text.toString()
          val context = activity.applicationContext
          Thread(Runnable {
            val values = ContentValues()
            values.put(ListColumns.TITLE, name)
            context.contentResolver.insert(Lists.CONTENT_URI, values)
          }).start()
        }
        .setNegativeButton(R.string.cancel) { _, _ -> }

    return builder.create()
  }
}
