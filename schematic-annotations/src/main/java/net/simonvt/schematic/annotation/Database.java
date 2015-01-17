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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * The class annotated describes a SQLiteOpenHelper.
 * <pre>{@code
 * &#064;Database(version = NotesDatabase.VERSION)
 * public final class NotesDatabase {
 *
 *   public static final int VERSION = 1;
 *
 *   &#064;Table(ListColumns.class) public static final String LISTS = "lists";
 *
 *   &#064;Table(NoteColumns.class) public static final String NOTES = "notes";
 * }
 * }</pre>
 */
@Retention(CLASS) @Target(TYPE)
public @interface Database {
  String className() default "";

  String fileName() default "";

  String packageName() default "";

  int version();
}
