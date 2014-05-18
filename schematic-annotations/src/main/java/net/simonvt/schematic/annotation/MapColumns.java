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
 * Maps a column or subquery to another column.
 * <pre>{@code
 * &#064;MapColumns public static Map<String, String> mapColumns() {
 *   Map<String, String> map = new HashMap<String, String>();
 *   map.put(from, to);
 *   return map;
 * }
 * }</pre>
 *
 * This is then inserted into the columns in the select statement:
 *
 * <pre>
 * SELECT (to AS from) FROM table
 * </pre>
 */
@Retention(CLASS) @Target(METHOD)
public @interface MapColumns {
}
