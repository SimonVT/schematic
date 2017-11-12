package net.simonvt.schematic.compiler;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Table;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Created by orlanth23 on 10/11/2017.
 */

public class DbDescriptionTable {
    @Table(DbDescriptionInterface.class)
    @IfNotExists
    public static final String LISTS = "lists";

    public interface DbDescriptionInterface {

        @DataType(INTEGER)
        @AutoIncrement
        @PrimaryKey
        @NotNull
        String ID_DESCRIPTION = "id_description";

        @DataType(INTEGER)
        @NotNull
        String DB_VERSION = "db_version";

        @DataType(TEXT)
        @NotNull
        String TABLE_NAME = "table_name";

        @DataType(TEXT)
        String COLUMN_NAME = "column_name";

        @DataType(TEXT)
        String COLUMN_TYPE = "column_type";


    }
}
