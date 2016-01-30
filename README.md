Schematic
=========

Automatically generate a ContentProvider backed by an SQLite database.


Usage
=====

First create a class that contains the columns of a database table.

```java
public interface ListColumns {

  @DataType(INTEGER) @PrimaryKey @AutoIncrement String _ID = "_id";

  @DataType(TEXT) @NotNull String TITLE = "title";
}
```


Then create a database that uses this column

```java
@Database(version = NotesDatabase.VERSION)
public final class NotesDatabase {

  public static final int VERSION = 1;

  @Table(ListColumns.class) public static final String LISTS = "lists";
}
```


And finally define a ContentProvider

```java
@ContentProvider(authority = NotesProvider.AUTHORITY, database = NotesDatabase.class)
public final class NotesProvider {

  public static final String AUTHORITY = "net.simonvt.schematic.sample.NotesProvider";

  @TableEndpoint(table = NotesDatabase.LISTS) public static class Lists {

    @ContentUri(
        path = "lists",
        type = "vnd.android.cursor.dir/list",
        defaultSort = ListColumns.TITLE + " ASC")
    public static final Uri LISTS = Uri.parse("content://" + AUTHORITY + "/lists");
  }
```

Table column annotations
------------------------

```java
@AutoIncrement
@DataType
@DefaultValue
@NotNull
@PrimaryKey
@References
@Unique
```

Defining an Uri
---------------

The ```@ContentUri``` annotation is used when the ```Uri``` does not change.

```java
@ContentUri(
  path = "lists",
  type = "vnd.android.cursor.dir/list",
  defaultSort = ListColumns.TITLE + " ASC")
public static final Uri LISTS = Uri.parse("content://" + AUTHORITY + "/lists");
```

If the ```Uri``` is created based on some value, e.g. an id, ```@InexactContentUri``` is used.
```java
@InexactContentUri(
  path = Path.LISTS + "/#",
  name = "LIST_ID",
  type = "vnd.android.cursor.item/list",
  whereColumn = ListColumns._ID,
  pathSegment = 1)
public static Uri withId(long id) {
  return Uri.parse("content://" + AUTHORITY + "/lists/" + id);
}
```


Including in your project
=========================

I recommend using the android-apt plugin. It doesn't include the compiler artifact in the final apk,
and also sets the source paths so AS picks it up the generated classes.

```groovy
apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt'

buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath 'com.android.tools.build:gradle:{latest-version}'
    classpath 'com.neenbedankt.gradle.plugins:android-apt:{latest-version}'
  }
}

dependencies {
  apt 'net.simonvt.schematic:schematic-compiler:{latest-version}'
  compile 'net.simonvt.schematic:schematic:{latest-version}'
}
```


License
=======

    Copyright 2014 Simon Vig Therkildsen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
