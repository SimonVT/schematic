package net.simonvt.schematic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Adds the UNIQUE keyword to the database column.
 */
@Retention(CLASS) @Target(FIELD)
public @interface Unique {
}
