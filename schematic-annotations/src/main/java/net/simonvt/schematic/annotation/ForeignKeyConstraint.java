package net.simonvt.schematic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/** Adds FOREIGN KEY constraint on table level */
@Retention(CLASS) @Target(TYPE)
public @interface ForeignKeyConstraint {
    /** Optional name for constraint */
    String name() default "";

    /** Column names to be used in constraint */
    String[] columns();

    /** Name of referenced table */
    String referencedTable();

    /** Column names in referenced table */
    String[] referencedColumns();

    /**
     * Defines conflict resolution algorithm.
     * By default {@link ConflictResolutionType#NONE} is used.
     * */
    ConflictResolutionType onConflict() default ConflictResolutionType.NONE;
}
