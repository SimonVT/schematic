package net.simonvt.schematic.sample.database;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.Constraints;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DataType.Type;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;
import net.simonvt.schematic.annotation.UniqueConstraint;

import static net.simonvt.schematic.annotation.ConflictResolutionType.REPLACE;

@Constraints(
    unique = @UniqueConstraint(
            name = "UNQ_TAG_FOR_NOTE",
            columns = {TagColumns.NOTE_ID, TagColumns.NAME},
            onConflict = REPLACE)
)
public interface TagColumns {
  @DataType(Type.INTEGER)
  @PrimaryKey
  @AutoIncrement
  String ID = "_id";

  @DataType(Type.INTEGER)
  @NotNull
  @References(table = NotesDatabase.NOTES, column = NoteColumns.ID)
  String NOTE_ID = "note_id";

  @DataType(Type.TEXT)
  @NotNull
  String NAME = "name";
}
