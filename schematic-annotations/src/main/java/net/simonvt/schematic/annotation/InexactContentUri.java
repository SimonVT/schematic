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

package net.simonvt.schematic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * An inexact content URI. Annotates the method used to construct the URI.
 * <pre>{@code
 * &#064;InexactContentUri(
 *   path = Path.LISTS + "/#",
 *   name = "LIST_ID",
 *   type = "vnd.android.cursor.item/list",
 *   whereColumn = ListColumns._ID,
 *   pathSegment = 1)
 * public static Uri withId(long id) {
 *   return Uri.parse("content://" + AUTHORITY + "/lists/" + id);
 * }
 * }</pre>
 */
@Retention(CLASS) @Target(METHOD)
public @interface InexactContentUri {
  String path();

  String type();

  String name();

  String[] whereColumn();

  int[] pathSegment();

  String[] where() default { };

  String table() default "";

  String join() default "";

  String defaultSort() default "";

  String groupBy() default "";

  String having() default "";

  String limit() default "";

  boolean allowQuery() default true;

  boolean allowUpdate() default true;

  boolean allowDelete() default true;

  boolean allowInsert() default true;
}
