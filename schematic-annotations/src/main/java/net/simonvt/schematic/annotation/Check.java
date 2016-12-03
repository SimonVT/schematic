package net.simonvt.schematic.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Add CHECK constraint. Can be used on column level or on table level.
 * <pre>
 * <code>
 *
 * &#64;Check(NotesColumns.STATUS + " = '" + NotesColumns.STATUS_COMPLETED + "' and " + NotesColumns.COMPLETION_DATE + " is not null")
 * public interface NotesColumns{
 *   String STATUS_NEW = "new";
 *   String STATUS_COMPLETED = "completed";
 *   &#64;DataType(TEXT) &#64;CHECK(NotesColumns.STATUS + " in ('"+NotesColumns.STATUS_NEW+"', '"+NotesColumns.STATUS_COMPLETED+"')") String STATUS = "status";
 *   &#64;DataType(TEXT) String COMPLETION_DATE = "completion_date";
 * }
 *
 * </code>
 * </pre>
 */
@Retention(CLASS) @Target({TYPE, FIELD})
public @interface Check {
    /**
     * CHECK constraint text. Shouldn't contain double quotes. Use single quote instead.
     */
    String value();

    /** optional name for constraint*/
    String name() default "";
}
