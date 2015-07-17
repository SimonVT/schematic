/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.schematic.compiler;

import com.squareup.javapoet.ClassName;

public interface Clazz {

  ClassName CONTEXT = ClassName.get("android.content", "Context");

  ClassName CURSOR = ClassName.get("android.database", "Cursor");

  ClassName CONTENT_PROVIDER = ClassName.get("android.content", "ContentProvider");

  ClassName CONTENT_URIS = ClassName.get("android.content", "ContentUris");

  ClassName CONTENT_VALUES = ClassName.get("android.content", "ContentValues");

  ClassName CONTENT_PROVIDER_OPERATION =
      ClassName.get("android.content", "ContentProviderOperation");

  ClassName CONTENT_PROVIDER_RESULT = ClassName.get("android.content", "ContentProviderResult");

  ClassName OPERATION_APPLICATION_EXCEPTION =
      ClassName.get("android.content", "OperationApplicationException");

  ClassName SELECTION_BUILDER = ClassName.get("net.simonvt.schematic.utils", "SelectionBuilder");

  ClassName SQLITE_DATABASE = ClassName.get("android.database.sqlite", "SQLiteDatabase");

  ClassName SQLITE_OPEN_HELPER = ClassName.get("android.database.sqlite", "SQLiteOpenHelper");

  ClassName URI = ClassName.get("android.net", "Uri");

  ClassName URI_MATCHER = ClassName.get("android.content", "UriMatcher");
}
