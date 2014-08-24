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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * A content URI is a URI that identifies data in a provider. This has to be used inside a class
 * annotated with {@link net.simonvt.schematic.annotation.TableEndpoint},
 * <pre>{@code
 * &#064;ContentUri(
 *   path = "lists",
 *   type = "vnd.android.cursor.dir/list",
 *   defaultSort = ListColumns.TITLE + " ASC")
 * public static final Uri LISTS = Uri.parse("content://" + AUTHORITY + "/lists");
 * }</pre>
 */
@Retention(CLASS) @Target(FIELD)
public @interface ContentUri {
  String path();

  String type();

  String[] where() default { };

  String table() default "";

  String join() default "";

  String defaultSort() default "";

  String groupBy() default "";

  String having() default "";

  String limit() default "";

  boolean allowQuery() default true;

  boolean allowInsert() default true;

  boolean allowUpdate() default true;

  boolean allowDelete() default true;
}
