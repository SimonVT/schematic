/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.torrenttoise.util

import android.database.Cursor

inline fun Cursor.getBlob(column: String): ByteArray = this.getBlob(
    this.getColumnIndexOrThrow(column))

inline fun Cursor.getDouble(column: String): Double = this.getDouble(
    this.getColumnIndexOrThrow(column))

inline fun Cursor.getFloat(column: String): Float = this.getFloat(
    this.getColumnIndexOrThrow(column))

inline fun Cursor.getInt(column: String): Int = this.getInt(this.getColumnIndexOrThrow(column))
inline fun Cursor.getLong(column: String): Long = this.getLong(this.getColumnIndexOrThrow(column))
inline fun Cursor.getString(column: String): String = this.getString(
    this.getColumnIndexOrThrow(column))
