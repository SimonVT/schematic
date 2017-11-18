package net.simonvt.schematic.sample.database

import net.simonvt.schematic.annotation.*
import net.simonvt.schematic.annotation.ConflictResolutionType.REPLACE
import net.simonvt.schematic.annotation.DataType.Type

@Constraints(
    unique = arrayOf(
        UniqueConstraint(
            name = "UNQ_TAG_FOR_NOTE",
            columns = arrayOf(TagColumns.NOTE_ID, TagColumns.NAME),
            onConflict = REPLACE
        )
    )
)
interface TagColumns {
  companion object {
    @DataType(Type.INTEGER) @PrimaryKey @AutoIncrement const val ID = "_id"

    @DataType(Type.INTEGER) @NotNull
    @References(table = NotesDatabase.NOTES, column = NoteColumns.ID,
            onDelete = ForeignKeyAction.CASCADE)
    const val NOTE_ID = "note_id"

    @DataType(Type.TEXT) @NotNull
    const val NAME = "name"
  }
}
