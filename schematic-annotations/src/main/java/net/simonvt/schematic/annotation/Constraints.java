package net.simonvt.schematic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a list of {@link UniqueConstraint} that should be applied on the table level
 */
@Retention(RetentionPolicy.SOURCE) @Target(ElementType.TYPE)
public @interface Constraints {

    /** list of unique constraints */
    UniqueConstraint[] unique() default {};

    /** list of check constraints */
    Check[] check() default {};

    /** list of foreign key constraints */
    ForeignKeyConstraint[] foreignKey() default {};
}
