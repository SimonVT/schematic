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

package net.simonvt.schematic.compiler;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.InsertUri;
import net.simonvt.schematic.annotation.MapColumns;
import net.simonvt.schematic.annotation.NotificationUri;
import net.simonvt.schematic.annotation.NotifyDelete;
import net.simonvt.schematic.annotation.NotifyInsert;
import net.simonvt.schematic.annotation.NotifyUpdate;
import net.simonvt.schematic.annotation.TableEndpoint;
import net.simonvt.schematic.annotation.Where;

public class ContentProviderWriter {

  static class UriContract {

    static enum Type {
      EXACT,
      INEXACT
    }

    Type contractType;

    String path;

    String name;

    String classQualifiedName;

    String table;

    String join;

    String type;

    String defaultSort;

    String groupBy;

    String having;

    String limit;

    String[] where;

    String[] whereColumns;

    int[] pathSegments;

    Element parent;

    boolean allowQuery;
    boolean allowInsert;
    boolean allowUpdate;
    boolean allowDelete;

    UriContract(Type type) {
      this.contractType = type;
    }
  }

  ProcessingEnvironment processingEnv;
  Elements elementUtils;

  String outPackage;

  // Class describing the ContentProvider
  Element provider;
  String descriptorPackage;
  String authority;

  // Package and class className of generated provider
  // String providerPackage;
  String providerName;

  Element database;
  String databaseName;

  ExecutableElement defaultNotifyInsert;
  ExecutableElement defaultNotifyUpdate;
  ExecutableElement defaultNotifyDelete;

  List<UriContract> uris = new ArrayList<UriContract>();
  List<String> paths = new ArrayList<String>();
  Map<String, Element> notificationUris = new HashMap<String, Element>();
  Map<String, ExecutableElement> notifyInsert = new HashMap<String, ExecutableElement>();
  Map<String, ExecutableElement> notifyUpdate = new HashMap<String, ExecutableElement>();
  Map<String, ExecutableElement> notifyDelete = new HashMap<String, ExecutableElement>();
  Map<String, ExecutableElement> whereCalls = new HashMap<String, ExecutableElement>();
  Map<String, ExecutableElement> insertUris = new HashMap<String, ExecutableElement>();

  Map<Element, ExecutableElement> columnMaps = new HashMap<Element, ExecutableElement>();

  public ContentProviderWriter(ProcessingEnvironment processingEnv, Elements elements,
      Element provider) {
    this.processingEnv = processingEnv;
    this.provider = provider;
    elementUtils = processingEnv.getElementUtils();

    TypeElement elm = (TypeElement) provider;
    descriptorPackage = getPackageName(elm);

    ContentProvider annotation = provider.getAnnotation(ContentProvider.class);
    this.authority = annotation.authority();

    this.providerName = annotation.name();
    if (providerName.trim().isEmpty()) {
      providerName = provider.getSimpleName().toString();
    }

    this.outPackage = annotation.packageName();
    if (outPackage.trim().isEmpty()) {
      this.outPackage = elements.getPackageOf(provider).getQualifiedName() + ".generated";
    }

    // Get database name
    try {
      annotation.database();
    } catch (MirroredTypeException e) {
      TypeMirror mirror = e.getTypeMirror();
      this.database = processingEnv.getTypeUtils().asElement(mirror);
      String databaseSchematicName = this.database.getSimpleName().toString();
      Database database = this.database.getAnnotation(Database.class);
      databaseName = database.className();
      if (databaseName.trim().isEmpty()) {
        this.databaseName = databaseSchematicName;
      }
    }

    List<? extends Element> enclosedElements = provider.getEnclosedElements();

    for (Element enclosedElement : enclosedElements) {
      NotifyInsert defaultNotifyInsert = enclosedElement.getAnnotation(NotifyInsert.class);
      if (defaultNotifyInsert != null) {
        this.defaultNotifyInsert = (ExecutableElement) enclosedElement;
      }
      NotifyUpdate defaultNotifyUpdate = enclosedElement.getAnnotation(NotifyUpdate.class);
      if (defaultNotifyUpdate != null) {
        this.defaultNotifyUpdate = (ExecutableElement) enclosedElement;
      }
      NotifyDelete defaultNotifyDelete = enclosedElement.getAnnotation(NotifyDelete.class);
      if (defaultNotifyDelete != null) {
        this.defaultNotifyDelete = (ExecutableElement) enclosedElement;
      }

      TableEndpoint tableEndpoint = enclosedElement.getAnnotation(TableEndpoint.class);
      if (tableEndpoint != null) {
        final String table = tableEndpoint.table();

        // Get uri's
        List<? extends Element> contentUris = enclosedElement.getEnclosedElements();
        for (Element element : contentUris) {
          ContentUri contentUri = element.getAnnotation(ContentUri.class);
          if (contentUri != null) {
            UriContract contract = new UriContract(UriContract.Type.EXACT);
            String path = contentUri.path().trim();

            if (path.isEmpty()) {
              error("Empty path for " + getFullyQualified(element));
            }

            if (!paths.contains(path)) {
              paths.add(path);
            } else {
              error("Duplicate path " + path);
            }

            contract.path = path;

            String parent = ((TypeElement) enclosedElement).getQualifiedName().toString();
            contract.name = enclosedElement.getSimpleName().toString().toUpperCase()
                + "_"
                + element.getSimpleName().toString();
            contract.classQualifiedName = parent;

            String contentTable = contentUri.table();
            if (!contentTable.trim().isEmpty()) {
              contract.table = contentTable;
            } else {
              contract.table = table;
            }

            String join = contentUri.join();
            if (!join.trim().isEmpty()) {
              contract.join = join;
            }

            contract.type = contentUri.type();

            contract.where = contentUri.where();

            String defaultSort = contentUri.defaultSort();
            if (!defaultSort.trim().isEmpty()) {
              contract.defaultSort = defaultSort;
            }

            String groupBy = contentUri.groupBy();
            if (!groupBy.trim().isEmpty()) {
              contract.groupBy = groupBy;
            }

            String having = contentUri.having();
            if (!having.trim().isEmpty()) {
              contract.having = having;
            }

            String limit = contentUri.limit();
            if (!limit.trim().isEmpty()) {
              contract.limit = limit;
            }

            contract.allowQuery = contentUri.allowQuery();
            contract.allowInsert = contentUri.allowInsert();
            contract.allowUpdate = contentUri.allowUpdate();
            contract.allowDelete = contentUri.allowDelete();

            contract.parent = enclosedElement;

            uris.add(contract);
          }

          InexactContentUri inexactUri = element.getAnnotation(InexactContentUri.class);
          if (inexactUri != null) {
            UriContract contract = new UriContract(UriContract.Type.INEXACT);
            String path = inexactUri.path().trim();

            if (path.isEmpty()) {
              error("Empty path for " + getFullyQualified(element));
            }

            contract.path = path;
            if (!paths.contains(path)) {
              paths.add(path);
            } else {
              error("Duplicate path " + path);
            }

            contract.path = path;

            contract.whereColumns = inexactUri.whereColumn();
            contract.pathSegments = inexactUri.pathSegment();

            String parent = ((TypeElement) enclosedElement).getQualifiedName().toString();
            contract.name =
                enclosedElement.getSimpleName().toString().toUpperCase() + "_" + inexactUri.name();
            contract.classQualifiedName = parent;

            String contentTable = inexactUri.table();
            if (!contentTable.trim().isEmpty()) {
              contract.table = contentTable;
            } else {
              contract.table = table;
            }

            String join = inexactUri.join();
            if (!join.trim().isEmpty()) {
              contract.join = join;
            }

            contract.type = inexactUri.type();

            contract.where = inexactUri.where();

            String defaultSort = inexactUri.defaultSort();
            if (!defaultSort.trim().isEmpty()) {
              contract.defaultSort = defaultSort;
            }

            String groupBy = inexactUri.groupBy();
            if (!groupBy.trim().isEmpty()) {
              contract.groupBy = groupBy;
            }

            String having = inexactUri.having();
            if (!having.trim().isEmpty()) {
              contract.having = having;
            }

            String limit = inexactUri.limit();
            if (!limit.trim().isEmpty()) {
              contract.limit = limit;
            }

            contract.allowQuery = inexactUri.allowQuery();
            contract.allowInsert = inexactUri.allowInsert();
            contract.allowUpdate = inexactUri.allowUpdate();
            contract.allowDelete = inexactUri.allowDelete();

            contract.parent = enclosedElement;

            uris.add(contract);
          }

          NotifyInsert notifyInsert = element.getAnnotation(NotifyInsert.class);
          if (notifyInsert != null) {
            String[] paths = notifyInsert.paths();
            for (String path : paths) {
              this.notifyInsert.put(path, (ExecutableElement) element);
            }
          }

          NotifyUpdate notifyUpdate = element.getAnnotation(NotifyUpdate.class);
          if (notifyUpdate != null) {
            String[] paths = notifyUpdate.paths();
            for (String path : paths) {
              this.notifyUpdate.put(path, (ExecutableElement) element);
            }
          }

          NotifyDelete notifyDelete = element.getAnnotation(NotifyDelete.class);
          if (notifyDelete != null) {
            String[] paths = notifyDelete.paths();
            for (String path : paths) {
              this.notifyDelete.put(path, (ExecutableElement) element);
            }
          }

          Where where = element.getAnnotation(Where.class);
          if (where != null) {
            this.whereCalls.put(where.path(), (ExecutableElement) element);
          }

          NotificationUri notificationUri = element.getAnnotation(NotificationUri.class);
          if (notificationUri != null) {
            String[] paths = notificationUri.paths();
            for (String path : paths) {
              if (notificationUris.containsKey(paths)) {
                error("Multiple NotificationUri's for path '" + path + "' defined");
              }

              notificationUris.put(path, element);
            }
          }

          InsertUri insertUri = element.getAnnotation(InsertUri.class);
          if (insertUri != null) {
            String[] paths = insertUri.paths();
            for (String path : paths) {
              insertUris.put(path, (ExecutableElement) element);
            }
          }

          MapColumns mapColumns = element.getAnnotation(MapColumns.class);
          if (mapColumns != null) {
            columnMaps.put(enclosedElement, (ExecutableElement) element);
          }
        }
      }
    }

    checkPathsExist(notificationUris);
    checkPathsExist(notifyInsert);
    checkPathsExist(notifyUpdate);
    checkPathsExist(notifyDelete);
    checkPathsExist(whereCalls);
    checkPathsExist(insertUris);
  }

  private void checkPathsExist(Map<String, ? extends Element> map) {
    Set<String> paths = map.keySet();
    for (String path : paths) {
      if (!this.paths.contains(path)) {
        error("Unknown path " + path + " for " + getFullyQualified(map.get(path)));
      }
    }
  }

  public void write(Filer filer) throws IOException {
    JavaFileObject jfo = filer.createSourceFile(getFileName());
    Writer out = jfo.openWriter();
    JavaWriter writer = new JavaWriter(out);

    writer.emitPackage(outPackage);

    writer.emitImports("android.content.ContentProvider")
        .emitImports("android.content.UriMatcher")
        .emitImports("android.net.Uri")
        .emitImports("android.content.ContentProviderOperation")
        .emitImports("android.content.ContentProviderResult")
        .emitImports("android.content.ContentValues")
        .emitImports("android.content.ContentUris")
        .emitImports("android.content.OperationApplicationException")
        .emitImports("android.database.Cursor")
        .emitImports("android.database.sqlite.SQLiteDatabase")
        .emitImports("java.util.ArrayList")
        .emitImports("java.util.Map")
        .emitImports("java.util.Set")
        .emitImports("net.simonvt.schematic.utils.SelectionBuilder")
        .emitImports(outPackage + "." + databaseName)
        .emitEmptyLine();

    writer.beginType(providerName, "class", EnumSet.of(Modifier.PUBLIC), "ContentProvider")
        .emitEmptyLine()
        .emitField("String", "AUTHORITY",
            EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL), "\"" + authority + "\"")
        .emitEmptyLine();

    // Generate matcher IDs
    int code = 0;
    for (UriContract uri : uris) {
      writer.emitField("int", uri.name,
          EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL), String.valueOf(code));
      code++;
    }
    writer.emitEmptyLine();

    // Set up URI_MATCHER
    writer.emitField("UriMatcher", "MATCHER",
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL),
        "new UriMatcher(UriMatcher.NO_MATCH)").beginInitializer(true);

    for (UriContract uri : uris) {
      String path;
      if (uri.path != null) {
        path = "\"" + uri.path + "\"";
      } else {
        path = String.format("%s.%s.getPath()", uri.classQualifiedName, uri.name);
      }
      writer.emitStatement("MATCHER.addURI(AUTHORITY, %s, %s)", path, uri.name);
    }

    writer.endInitializer().emitEmptyLine();

    writer.emitField(databaseName, "database").emitEmptyLine();
    // onCreate
    writer.emitAnnotation(Override.class)
        .beginMethod("boolean", "onCreate", EnumSet.of(Modifier.PUBLIC))
        .emitStatement("database = %s.getInstance(getContext())", databaseName)
        .emitStatement("return true")
        .endMethod();

    // getBuilder
    writer.beginMethod("SelectionBuilder", "getBuilder", EnumSet.of(Modifier.PRIVATE), "String",
        "table").emitStatement("SelectionBuilder builder = new SelectionBuilder()");

    Set<Element> tableKeys = columnMaps.keySet();
    for (Element key : tableKeys) {
      ExecutableElement method = columnMaps.get(key);
      String parent = ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
      String methodName = method.getSimpleName().toString();

      writer.beginControlFlow("if (\"" + key.getSimpleName().toString() + "\".equals(table))");

      writer.emitStatement("Map<String, String> columnMap = %s.%s()", parent, methodName);
      writer.emitStatement("Set<String> keys = columnMap.keySet()")
          .beginControlFlow("for (String from : keys)")
          .emitStatement("String to = columnMap.get(from)")
          .emitStatement("builder.map(from, to)")
          .endControlFlow();

      writer.endControlFlow();
    }

    writer.emitStatement("return builder").endMethod().emitEmptyLine();

    writer.beginMethod("void", "insertValues", EnumSet.of(Modifier.PRIVATE), "SQLiteDatabase", "db",
        "String", "table", "ContentValues[]", "values");

    writer.beginControlFlow("for (ContentValues cv : values)")
        .emitStatement("db.insertOrThrow(table, null, cv)")
        .endControlFlow();

    writer.endMethod().emitEmptyLine();

    // Bulk insert
    writer.emitAnnotation(Override.class)
        .beginMethod("int", "bulkInsert", EnumSet.of(Modifier.PUBLIC), "Uri", "uri",
            "ContentValues[]", "values")
        .emitField("SQLiteDatabase", "db", EnumSet.of(Modifier.FINAL),
            "database.getWritableDatabase()")
        .emitStatement("db.beginTransaction()")
        .beginControlFlow("try")
        .emitEmptyLine();

    writer.beginControlFlow("switch(MATCHER.match(uri))");
    for (UriContract uri : uris) {
      if (uri.allowInsert) {
        writer.beginControlFlow("case " + uri.name + ":")
            .emitStatement("insertValues(db, \"%s\", values)", uri.table)
            .emitStatement("break")
            .endControlFlow();
      }
    }
    writer.endControlFlow();

    writer.emitEmptyLine()
        .emitStatement("db.setTransactionSuccessful()")
        .nextControlFlow("finally")
        .emitStatement("db.endTransaction()")
        .endControlFlow()
        .emitStatement("getContext().getContentResolver().notifyChange(uri, null)")
        .emitStatement("return values.length")
        .endMethod()
        .emitEmptyLine();

    // Apply batch
    writer.emitAnnotation(Override.class)
        .beginMethod("ContentProviderResult[]", "applyBatch", EnumSet.of(Modifier.PUBLIC),
            Arrays.asList("ArrayList<ContentProviderOperation>", "ops"),
            Arrays.asList("OperationApplicationException"))
        .emitStatement("ContentProviderResult[] results")
        .emitStatement("database.getWritableDatabase().beginTransaction()")
        .beginControlFlow("try")
        .emitStatement("results = super.applyBatch(ops)")
        .emitStatement("database.getWritableDatabase().setTransactionSuccessful()")
        .nextControlFlow("finally")
        .emitStatement("database.getWritableDatabase().endTransaction()")
        .endControlFlow()
        .emitStatement("return results")
        .endMethod();

    // getType
    writer.emitAnnotation(Override.class)
        .beginMethod("String", "getType", EnumSet.of(Modifier.PUBLIC), "Uri", "uri")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      writer.beginControlFlow("case " + uri.name + ":")
          .emitStatement("return \"" + uri.type + "\"")
          .endControlFlow();
    }

    writer.beginControlFlow("default:")
        .emitStatement("throw new IllegalArgumentException(\"Unknown URI \" + uri)")
        .endControlFlow()
        .endControlFlow()
        .endMethod()
        .emitEmptyLine();

    // query
    writer.emitAnnotation(Override.class)
        .beginMethod("Cursor", "query", EnumSet.of(Modifier.PUBLIC), "Uri", "uri", "String[]",
            "projection", "String", "selection", "String[]", "selectionArgs", "String", "sortOrder")
        .emitField("SQLiteDatabase", "db", EnumSet.of(Modifier.FINAL),
            "database.getReadableDatabase()")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowQuery) {
        writer.beginControlFlow("case " + uri.name + ":");
        writer.emitStatement("SelectionBuilder builder = getBuilder(\"%s\")",
            uri.parent.getSimpleName().toString());

        if (uri.defaultSort != null) {
          writer.beginControlFlow("if (sortOrder == null)")
              .emitStatement("sortOrder = \"%s\"", uri.defaultSort)
              .endControlFlow();
        }

        StringBuilder whereBuilder = new StringBuilder();
        if (uri.contractType == UriContract.Type.INEXACT) {
          for (int i = 0; i < uri.whereColumns.length; i++) {
            String column = uri.whereColumns[i];
            int pathSegment = uri.pathSegments[i];
            whereBuilder.append(".where(\"")
                .append(column)
                .append("=?\" , uri.getPathSegments().get(")
                .append(pathSegment)
                .append("))\n");
          }
        }
        for (String where : uri.where) {
          whereBuilder.append(".where(\"").append(where).append("\")\n");
        }
        whereBuilder.append(".where(selection, selectionArgs)\n");

        ExecutableElement where = whereCalls.get(uri.path);
        if (where != null) {
          String parent = ((TypeElement) where.getEnclosingElement()).getQualifiedName().toString();
          String methodName = where.getSimpleName().toString();

          List<? extends VariableElement> parameters = where.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
          }

          writer.emitStatement("String[] wheres = %s.%s(%s)", parent, methodName,
              params.toString());
          writer.beginControlFlow("for (String where : wheres)")
              .emitStatement("builder.where(where)")
              .endControlFlow();
        }

        StringBuilder tableBuilder = new StringBuilder(uri.table);
        if (uri.join != null) {
          tableBuilder.append(" ").append(uri.join);
        }

        if (uri.groupBy != null) {
          writer.emitField("String", "groupBy", EnumSet.of(Modifier.FINAL),
              "\"" + uri.groupBy + "\"");
        } else {
          writer.emitField("String", "groupBy", EnumSet.of(Modifier.FINAL), "null");
        }

        if (uri.having != null) {
          writer.emitField("String", "having", EnumSet.of(Modifier.FINAL),
              "\"" + uri.having + "\"");
        } else {
          writer.emitField("String", "having", EnumSet.of(Modifier.FINAL), "null");
        }

        if (uri.limit != null) {
          writer.emitField("String", "limit", EnumSet.of(Modifier.FINAL), "\"" + uri.limit + "\"");
        } else {
          writer.emitField("String", "limit", EnumSet.of(Modifier.FINAL), "null");
        }

        writer.emitStatement("Cursor cursor = builder.table(\"%s\")\n%s"
                + ".query(db, projection, groupBy, having, sortOrder, limit)",
            tableBuilder.toString(), whereBuilder.toString());

        Element notifyUri = notificationUris.get(uri.path);
        if (notifyUri != null) {
          String parent =
              ((TypeElement) notifyUri.getEnclosingElement()).getQualifiedName().toString();
          String uriName = notifyUri.getSimpleName().toString();
          writer.emitStatement("cursor.setNotificationUri(getContext().getContentResolver(), %s)",
              parent + "." + uriName);
        } else {
          writer.emitStatement("cursor.setNotificationUri(getContext().getContentResolver(), uri)");
        }

        writer.emitStatement("return cursor").endControlFlow();
      }
    }

    writer.beginControlFlow("default:")
        .emitStatement("throw new IllegalArgumentException(\"Unknown URI \" + uri)")
        .endControlFlow()
        .endControlFlow()
        .endMethod()
        .emitEmptyLine();

    // insert
    writer.emitAnnotation(Override.class)
        .beginMethod("Uri", "insert", EnumSet.of(Modifier.PUBLIC), "Uri", "uri", "ContentValues",
            "values")
        .emitField("SQLiteDatabase", "db", EnumSet.of(Modifier.FINAL),
            "database.getWritableDatabase()")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowInsert) {
        writer.beginControlFlow("case " + uri.name + ":")
            .emitStatement("final long id = db.insertOrThrow(\"%s\", null, values)", uri.table);

        if ((uri.path != null && notifyInsert.containsKey(uri.path))
            || defaultNotifyInsert != null) {
          ExecutableElement notifyMethod = notifyInsert.get(uri.path);
          if (notifyMethod == null) {
            notifyMethod = defaultNotifyInsert;
          }

          String parent =
              ((TypeElement) notifyMethod.getEnclosingElement()).getQualifiedName().toString();
          String methodName = notifyMethod.getSimpleName().toString();

          List<? extends VariableElement> parameters = notifyMethod.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.content.Context".equals(typeAsString)) {
              params.append("getContext()");
            }
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
            if ("android.content.ContentValues".equals(typeAsString)) {
              params.append("values");
            }
            if ("long".equals(typeAsString)) {
              params.append("id");
            }
            if ("java.lang.String".equals(typeAsString)) {
              params.append("where");
            }
            if ("java.lang.String[]".equals(typeAsString)) {
              params.append("whereArgs");
            }
          }

          writer.emitStatement("Uri[] notifyUris = %s.%s(%s)", parent, methodName,
              params.toString());

          writer.beginControlFlow("for (Uri notifyUri : notifyUris)")
              .emitStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
              .endControlFlow();
        } else {
          writer.emitStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        ExecutableElement insertUri = insertUris.get(uri.path);
        if (insertUri != null) {
          String parent =
              ((TypeElement) insertUri.getEnclosingElement()).getQualifiedName().toString();
          String methodName = insertUri.getSimpleName().toString();

          List<? extends VariableElement> parameters = insertUri.getParameters();

          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();

            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
            if ("android.content.ContentValues".equals(typeAsString)) {
              params.append("values");
            }
          }
          writer.emitStatement("return %s.%s(%s)", parent, methodName, params.toString());
        } else {
          writer.emitStatement("return ContentUris.withAppendedId(uri, id)");
        }

        writer.endControlFlow();
      }
    }

    writer.beginControlFlow("default:")
        .emitStatement("throw new IllegalArgumentException(\"Unknown URI \" + uri)")
        .endControlFlow()
        .endControlFlow()
        .endMethod()
        .emitEmptyLine();

    // update
    writer.emitAnnotation(Override.class)
        .beginMethod("int", "update", EnumSet.of(Modifier.PUBLIC), "Uri", "uri", "ContentValues",
            "values", "String", "where", "String[]", "whereArgs")
        .emitField("SQLiteDatabase", "db", EnumSet.of(Modifier.FINAL),
            "database.getWritableDatabase()")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowUpdate) {
        writer.beginControlFlow("case " + uri.name + ":");

        writer.emitStatement("SelectionBuilder builder = getBuilder(\"%s\")",
            uri.parent.getSimpleName().toString());

        if (uri.contractType == UriContract.Type.INEXACT) {
          for (int i = 0; i < uri.whereColumns.length; i++) {
            String column = uri.whereColumns[i];
            int pathSegment = uri.pathSegments[i];
            writer.emitStatement("builder.where(\"%s=?\", uri.getPathSegments().get(%d))", column,
                pathSegment);
          }
        }
        for (String where : uri.where) {
          writer.emitStatement("builder.where(\"%s\")", where);
        }
        writer.emitStatement("builder.where(where, whereArgs)");

        ExecutableElement where = whereCalls.get(uri.path);
        if (where != null) {
          String parent = ((TypeElement) where.getEnclosingElement()).getQualifiedName().toString();
          String methodName = where.getSimpleName().toString();

          List<? extends VariableElement> parameters = where.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
          }

          writer.emitStatement("String[] wheres = %s.%s(%s)", parent, methodName,
              params.toString());
          writer.beginControlFlow("for (String w : wheres)")
              .emitStatement("builder.where(w)")
              .endControlFlow();
        }

        boolean hasNotifyUris = false;
        if ((uri.path != null && notifyUpdate.containsKey(uri.path))
            || defaultNotifyUpdate != null) {
          hasNotifyUris = true;
          ExecutableElement notifyMethod = notifyUpdate.get(uri.path);
          if (notifyMethod == null) {
            notifyMethod = defaultNotifyUpdate;
          }

          String parent =
              ((TypeElement) notifyMethod.getEnclosingElement()).getQualifiedName().toString();
          String methodName = notifyMethod.getSimpleName().toString();

          List<? extends VariableElement> parameters = notifyMethod.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.content.Context".equals(typeAsString)) {
              params.append("getContext()");
            }
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
            if ("android.content.ContentValues".equals(typeAsString)) {
              params.append("values");
            }
            if ("java.lang.String".equals(typeAsString)) {
              params.append("builder.getSelection()");
            }
            if ("java.lang.String[]".equals(typeAsString)) {
              params.append("builder.getSelectionArgs()");
            }
          }

          writer.emitStatement("Uri[] notifyUris = %s.%s(%s)", parent, methodName,
              params.toString());
        }

        writer.emitStatement("final int count = builder.table(\"%s\")\n.update(db, values)",
            uri.table).beginControlFlow("if (count > 0)");

        if (hasNotifyUris) {
          writer.beginControlFlow("for (Uri notifyUri : notifyUris)")
              .emitStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
              .endControlFlow();
        } else {
          writer.emitStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        writer.endControlFlow().emitStatement("return count").endControlFlow();
      }
    }

    writer.beginControlFlow("default:")
        .emitStatement("throw new IllegalArgumentException(\"Unknown URI \" + uri)")
        .endControlFlow()
        .endControlFlow()
        .endMethod()
        .emitEmptyLine();

    // delete
    writer.emitAnnotation(Override.class)
        .beginMethod("int", "delete", EnumSet.of(Modifier.PUBLIC), "Uri", "uri", "String", "where",
            "String[]", "whereArgs")
        .emitField("SQLiteDatabase", "db", EnumSet.of(Modifier.FINAL),
            "database.getWritableDatabase()")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowDelete) {
        writer.beginControlFlow("case " + uri.name + ":");

        writer.emitStatement("SelectionBuilder builder = getBuilder(\"%s\")",
            uri.parent.getSimpleName().toString());

        if (uri.contractType == UriContract.Type.INEXACT) {
          for (int i = 0; i < uri.whereColumns.length; i++) {
            String column = uri.whereColumns[i];
            int pathSegment = uri.pathSegments[i];
            writer.emitStatement("builder.where(\"%s=?\", uri.getPathSegments().get(%d))", column,
                pathSegment);
          }
        }
        for (String where : uri.where) {
          writer.emitStatement("builder.where(\"%s\")", where);
        }
        writer.emitStatement("builder.where(where, whereArgs)");

        ExecutableElement where = whereCalls.get(uri.path);
        if (where != null) {
          String parent = ((TypeElement) where.getEnclosingElement()).getQualifiedName().toString();
          String methodName = where.getSimpleName().toString();

          List<? extends VariableElement> parameters = where.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
          }

          writer.emitStatement("String[] wheres = %s.%s(%s)", parent, methodName,
              params.toString());
          writer.beginControlFlow("for (String w : wheres)")
              .emitStatement("builder.where(w)")
              .endControlFlow();
        }

        boolean hasNotifyUris = false;
        if ((uri.path != null && notifyDelete.containsKey(uri.path))
            || defaultNotifyDelete != null) {
          hasNotifyUris = true;

          ExecutableElement notifyMethod = notifyDelete.get(uri.path);
          if (notifyMethod == null) {
            notifyMethod = defaultNotifyDelete;
          }

          String parent =
              ((TypeElement) notifyMethod.getEnclosingElement()).getQualifiedName().toString();
          String methodName = notifyMethod.getSimpleName().toString();

          List<? extends VariableElement> parameters = notifyMethod.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }
            TypeMirror paramType = param.asType();
            String typeAsString = paramType.toString();
            if ("android.content.Context".equals(typeAsString)) {
              params.append("getContext()");
            }
            if ("android.net.Uri".equals(typeAsString)) {
              params.append("uri");
            }
            if ("java.lang.String".equals(typeAsString)) {
              params.append("builder.getSelection()");
            }
            if ("java.lang.String[]".equals(typeAsString)) {
              params.append("builder.getSelectionArgs()");
            }
          }

          writer.emitStatement("Uri[] notifyUris = %s.%s(%s)", parent, methodName,
              params.toString());
        }

        writer.emitStatement("final int count = builder\n.table(\"%s\")\n.delete(db)", uri.table);

        if (hasNotifyUris) {
          writer.beginControlFlow("for (Uri notifyUri : notifyUris)")
              .emitStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
              .endControlFlow();
        } else {
          writer.emitStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        writer.emitStatement("return count").endControlFlow();
      }
    }

    writer.beginControlFlow("default:")
        .emitStatement("throw new IllegalArgumentException(\"Unknown URI \" + uri)")
        .endControlFlow()
        .endControlFlow()
        .endMethod()
        .emitEmptyLine();

    writer.endType();
    writer.close();
  }

  private String getFileName() {
    return outPackage + "." + providerName;
  }

  private String getPackageName(TypeElement type) {
    return elementUtils.getPackageOf(type).getQualifiedName().toString();
  }

  private String getFullyQualified(Element element) {
    String parent = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
    String elm = element.getSimpleName().toString();
    return parent + "." + elm;
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Kind.ERROR, error);
  }
}
