package net.simonvt.schematic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/** Adds the UNIQUE constraint on a table level */
@Retention(CLASS) @Target(TYPE)
public @interface UniqueConstraint {

    /** Optional name for constraint */
    String name() default "";

    /** Column names to be used in constraint */
    String[] columns();

    /**
     * Defines conflict resolution algorithm.
     * By default {@link ConflictResolutionType#NONE} is used.
     * */
    ConflictResolutionType onConflict() default ConflictResolutionType.NONE;
}
