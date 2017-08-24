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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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
import net.simonvt.schematic.annotation.Join;
import net.simonvt.schematic.annotation.MapColumns;
import net.simonvt.schematic.annotation.NotificationUri;
import net.simonvt.schematic.annotation.NotifyBulkInsert;
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
  ClassName databaseClass;

  ExecutableElement defaultNotifyInsert;
  ExecutableElement defaultNotifyBulkInsert;
  ExecutableElement defaultNotifyUpdate;
  ExecutableElement defaultNotifyDelete;

  List<UriContract> uris = new ArrayList<>();
  List<String> paths = new ArrayList<>();
  Map<String, Element> notificationUris = new HashMap<>();
  Map<String, ExecutableElement> notifyInsert = new HashMap<>();
  Map<String, ExecutableElement> notifyBulkInsert = new HashMap<>();
  Map<String, ExecutableElement> notifyUpdate = new HashMap<>();
  Map<String, ExecutableElement> notifyDelete = new HashMap<>();
  Map<String, ExecutableElement> joinCalls = new HashMap<>();
  Map<String, ExecutableElement> whereCalls = new HashMap<>();
  Map<String, ExecutableElement> insertUris = new HashMap<>();

  Map<Element, ExecutableElement> columnMaps = new HashMap<>();

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
      String databaseSchematicPackage = this.database.getEnclosingElement().getSimpleName().toString();
      Database database = this.database.getAnnotation(Database.class);
      if (database == null) {
        error("Database class "
            + this.database.toString()
            + ", referenced from ContentProvider "
            + provider.toString()
            + ", is missing @Database annotation");
      }
      String databaseName = database.className();
      String databasePackage = database.packageName();

      String resultingPackage = databaseSchematicPackage;
      String resultingName = databaseSchematicName;

      if (!databasePackage.trim().isEmpty()) {
        resultingPackage = databasePackage;
      }
      if (!databaseName.trim().isEmpty()) {
        resultingName = databaseName;
      }

      databaseClass = ClassName.get(resultingPackage, resultingName);
    }

    List<? extends Element> enclosedElements = provider.getEnclosedElements();

    for (Element enclosedElement : enclosedElements) {
      NotifyInsert defaultNotifyInsert = enclosedElement.getAnnotation(NotifyInsert.class);
      if (defaultNotifyInsert != null) {
        this.defaultNotifyInsert = (ExecutableElement) enclosedElement;
      }
      NotifyBulkInsert defaultNotifyBulkInsert =
          enclosedElement.getAnnotation(NotifyBulkInsert.class);
      if (defaultNotifyBulkInsert != null) {
        this.defaultNotifyBulkInsert = (ExecutableElement) enclosedElement;
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

          NotifyBulkInsert notifyBulkInsert = element.getAnnotation(NotifyBulkInsert.class);
          if (notifyBulkInsert != null) {
            String[] paths = notifyBulkInsert.paths();
            for (String path : paths) {
              this.notifyBulkInsert.put(path, (ExecutableElement) element);
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

          Join join = element.getAnnotation(Join.class);
          if (join != null) {
            this.joinCalls.put(join.path(), (ExecutableElement) element);
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

    TypeSpec.Builder contentResolver = TypeSpec.classBuilder(providerName)
        .superclass(Clazz.CONTENT_PROVIDER)
        .addModifiers(Modifier.PUBLIC);

    MethodSpec main = MethodSpec.methodBuilder("main")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(void.class)
        .addParameter(String[].class, "args")
        .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
        .build();

    FieldSpec authoritySpec =
        FieldSpec.builder(String.class, "AUTHORITY", Modifier.PUBLIC, Modifier.STATIC,
            Modifier.FINAL).initializer("$S", authority).build();
    contentResolver.addField(authoritySpec);

    // Generate matcher IDs
    for (int i = 0; i < uris.size(); i++) {
      UriContract uri = uris.get(i);
      FieldSpec idSpec =
          FieldSpec.builder(int.class, uri.name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
              .initializer(String.valueOf(i))
              .build();
      contentResolver.addField(idSpec);
    }

    // Set up URI_MATCHER
    FieldSpec matcherSpec =
        FieldSpec.builder(Clazz.URI_MATCHER, "MATCHER", Modifier.PRIVATE, Modifier.STATIC,
            Modifier.FINAL)
            .initializer("new $T($T.NO_MATCH)", Clazz.URI_MATCHER, Clazz.URI_MATCHER)
            .build();
    contentResolver.addField(matcherSpec);

    CodeBlock.Builder matcherInitializerBuilder = CodeBlock.builder();

    for (UriContract uri : uris) {
      String path;
      if (uri.path != null) {
        path = "\"" + uri.path + "\"";
      } else {
        path = String.format("%s.%s.getPath()", uri.classQualifiedName, uri.name);
      }
      matcherInitializerBuilder.addStatement("MATCHER.addURI(AUTHORITY, $L, $L)", path, uri.name);
    }

    CodeBlock matcherBlock = matcherInitializerBuilder.build();
    contentResolver.addStaticBlock(matcherBlock);

    // Database variable
    FieldSpec databaseSpec =
        FieldSpec.builder(Clazz.SQLITE_OPEN_HELPER, "database", Modifier.PRIVATE).build();
    contentResolver.addField(databaseSpec);

    // onCreate
    MethodSpec onCreateSpec = getOnCreateSpec();
    contentResolver.addMethod(onCreateSpec);

    // getBuilder
    contentResolver.addMethod(getBuilderSpec());
    contentResolver.addMethod(getInsertValuesSpec());
    contentResolver.addMethod(getBulkInsertSpec());
    contentResolver.addMethod(getApplyBatchSpec());
    contentResolver.addMethod(getTypeSpec());
    contentResolver.addMethod(getQuerySpec());
    contentResolver.addMethod(getInsertSpec());
    contentResolver.addMethod(getUpdateSpec());
    contentResolver.addMethod(getDeleteSpec());

    JavaFile javaFile = JavaFile.builder(outPackage, contentResolver.build()).build();
    javaFile.writeTo(out);
    out.flush();
    out.close();
  }

  private MethodSpec getOnCreateSpec() {
    return MethodSpec.methodBuilder("onCreate")
        .returns(boolean.class)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addStatement("database = $T.getInstance(getContext())", databaseClass)
        .addStatement("return true")
        .build();
  }

  private MethodSpec getBuilderSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("getBuilder")
        .returns(Clazz.SELECTION_BUILDER)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(String.class, "table")
        .addStatement("$T builder = new $T()", Clazz.SELECTION_BUILDER, Clazz.SELECTION_BUILDER);

    Set<Element> tableKeys = columnMaps.keySet();
    for (Element key : tableKeys) {
      ExecutableElement method = columnMaps.get(key);
      String parent = ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
      String methodName = method.getSimpleName().toString();

      spec.beginControlFlow("if ($S.equals(table))", key.getSimpleName().toString())
          .addStatement("$T columnMap = $L.$L()",
              ParameterizedTypeName.get(Map.class, String.class, String.class), parent, methodName)
          .addStatement("$T keys = columnMap.keySet()",
              ParameterizedTypeName.get(Set.class, String.class))
          .beginControlFlow("for (String from : keys)")
          .addStatement("String to = columnMap.get(from)")
          .addStatement("builder.map(from, to)")
          .endControlFlow()
          .endControlFlow();
    }

    spec.addStatement("return builder");

    return spec.build();
  }

  private MethodSpec getInsertValuesSpec() {
    return MethodSpec.methodBuilder("insertValues")
        .returns(ArrayTypeName.of(long.class))
        .addModifiers(Modifier.PRIVATE)
        .addParameter(Clazz.SQLITE_DATABASE, "db")
        .addParameter(String.class, "table")
        .addParameter(ArrayTypeName.of(Clazz.CONTENT_VALUES), "values")
        .addStatement("long[] ids = new long[values.length]")
        .beginControlFlow("for (int i = 0; i < values.length; i++)")
        .addStatement("$T cv = values[i]", Clazz.CONTENT_VALUES)
        .addStatement("db.insertOrThrow(table, null, cv)")
        .endControlFlow()
        .addStatement("return ids")
        .build();
  }

  private MethodSpec getBulkInsertSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("bulkInsert")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(int.class)
        .addParameter(Clazz.URI, "uri")
        .addParameter(ArrayTypeName.of(Clazz.CONTENT_VALUES), "values")
        .addStatement("final $T db = database.getWritableDatabase()", Clazz.SQLITE_DATABASE)
        .addStatement("db.beginTransaction()")
        .beginControlFlow("try")

        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowInsert) {
        spec.beginControlFlow("case $L:", uri.name)
            .addStatement("long[] ids = insertValues(db, $S, values)", uri.table);

        if ((uri.path != null && notifyBulkInsert.containsKey(uri.path))
            || defaultNotifyBulkInsert != null) {
          spec.addCode(getNotifyBulkInsert(uri));
        } else {
          spec.addStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        spec.addStatement("break").endControlFlow();
      }
    }

    spec.endControlFlow()
        .addStatement("db.setTransactionSuccessful()")
        .nextControlFlow("finally")
        .addStatement("db.endTransaction()")
        .endControlFlow()
        .addStatement("return values.length");

    return spec.build();
  }

  private MethodSpec getApplyBatchSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("applyBatch")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ArrayTypeName.of(Clazz.CONTENT_PROVIDER_RESULT));

    ClassName arrayList = ClassName.get(ArrayList.class);

    spec.addParameter(ParameterizedTypeName.get(arrayList, Clazz.CONTENT_PROVIDER_OPERATION), "ops")
        .addException(Clazz.OPERATION_APPLICATION_EXCEPTION);

    spec.addStatement("$T[] results", Clazz.CONTENT_PROVIDER_RESULT)
        .addStatement("final $T db = database.getWritableDatabase()", Clazz.SQLITE_DATABASE)
        .addStatement("db.beginTransaction()")
        .beginControlFlow("try")
        .addStatement("results = super.applyBatch(ops)")
        .addStatement("db.setTransactionSuccessful()")
        .nextControlFlow("finally")
        .addStatement("db.endTransaction()")
        .endControlFlow()
        .addStatement("return results");

    return spec.build();
  }

  private MethodSpec getTypeSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("getType")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(String.class)
        .addParameter(Clazz.URI, "uri")
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      spec.beginControlFlow("case $L:", uri.name)
          .addStatement("return $S", uri.type)
          .endControlFlow();
    }

    spec.beginControlFlow("default:")
        .addStatement("throw new $T(\"Unknown URI \" + uri)", IllegalArgumentException.class)
        .endControlFlow()
        .endControlFlow();

    return spec.build();
  }

  private MethodSpec getQuerySpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("query")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(Clazz.CURSOR)
        .addParameter(Clazz.URI, "uri")
        .addParameter(ArrayTypeName.of(String.class), "projection")
        .addParameter(String.class, "selection")
        .addParameter(ArrayTypeName.of(String.class), "selectionArgs")
        .addParameter(String.class, "sortOrder");

    spec.addStatement("final $T db = database.getReadableDatabase()", Clazz.SQLITE_DATABASE);

    spec.beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowQuery) {
        spec.beginControlFlow("case $L:", uri.name)
            .addStatement("$T builder = getBuilder($S)", Clazz.SELECTION_BUILDER,
                uri.parent.getSimpleName().toString());

        if (uri.defaultSort != null) {
          spec.beginControlFlow("if (sortOrder == null)")
              .addStatement("sortOrder = $S", uri.defaultSort)
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
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else {
              error(String.format("@Where does not support parameter %s", paramType.toString()));
            }
          }

          spec.addStatement("$T wheres = $L.$L($L)", ArrayTypeName.of(String.class), parent,
              methodName, params.toString())
              .beginControlFlow("for ($T where : wheres)", String.class)
              .addStatement("builder.where(where)")
              .endControlFlow();
        }

        if (uri.join != null) {
          spec.addStatement("$T join = \" \" + $S", String.class, uri.join);
        }

        ExecutableElement joins = joinCalls.get(uri.path);
        if (joins != null) {
          String parent = ((TypeElement) joins.getEnclosingElement()).getQualifiedName().toString();
          String methodName = joins.getSimpleName().toString();

          List<? extends VariableElement> parameters = joins.getParameters();
          StringBuilder params = new StringBuilder();
          boolean first = true;
          for (VariableElement param : parameters) {
            if (first) {
              first = false;
            } else {
              params.append(", ");
            }

            TypeMirror paramType = param.asType();
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else {
              error(String.format("@Join does not support parameter %s", paramType.toString()));
            }
          }

          if (uri.join == null) {
            spec.addStatement("$T join = \"\"", String.class);
          }

          spec.addStatement("$T joins = $L.$L($L)", ArrayTypeName.of(String.class), parent,
              methodName, params.toString())
              .beginControlFlow("for ($T j : joins)", String.class)
              .addStatement("join += \" \"")
              .addStatement("join += j")
              .endControlFlow();
        }

        if (uri.join != null || joins != null) {
          spec.addStatement("$T table = $S + join", String.class, uri.table);
        } else {
          spec.addStatement("$T table = $S", String.class, uri.table);
        }

        spec.addStatement("final String groupBy = $S", uri.groupBy)
            .addStatement("final String having = $S", uri.having)
            .addStatement("final String limit = $S", uri.limit);

        // TODO: The whereBuilder part is kind of gross
        spec.addStatement(
            "$T cursor = builder.table(table)\n$L.query(db, projection, groupBy, having, sortOrder, limit)",
            Clazz.CURSOR, whereBuilder.toString());

        Element notifyUri = notificationUris.get(uri.path);
        if (notifyUri != null) {
          String parent =
              ((TypeElement) notifyUri.getEnclosingElement()).getQualifiedName().toString();
          String uriName = notifyUri.getSimpleName().toString();
          spec.addStatement("cursor.setNotificationUri(getContext().getContentResolver(), $L)",
              parent + "." + uriName);
        } else {
          spec.addStatement("cursor.setNotificationUri(getContext().getContentResolver(), uri)");
        }

        spec.addStatement("return cursor").endControlFlow();
      }
    }

    spec.beginControlFlow("default:")
        .addStatement("throw new $T(\"Unknown URI \" + uri)", IllegalArgumentException.class)
        .endControlFlow()
        .endControlFlow();

    return spec.build();
  }

  private MethodSpec getInsertSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("insert")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(Clazz.URI)
        .addParameter(Clazz.URI, "uri")
        .addParameter(Clazz.CONTENT_VALUES, "values")
        .addStatement("final $T db = database.getWritableDatabase()", Clazz.SQLITE_DATABASE)
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowInsert) {
        spec.beginControlFlow("case $L:", uri.name)
            .addStatement("final $T id = db.insertOrThrow($S, null, values)", long.class,
                uri.table);

        if ((uri.path != null && notifyInsert.containsKey(uri.path))
            || defaultNotifyInsert != null) {
          CodeBlock notifyInsert = getNotifyInsert(uri);
          spec.addCode(notifyInsert);
        } else {
          spec.addStatement("getContext().getContentResolver().notifyChange(uri, null)");
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
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else if (Clazz.CONTENT_VALUES.equals(ClassName.get(paramType))) {
              params.append("values");
            } else {
              error(String.format("@NotifyInsert does not support parameter %s",
                  paramType.toString()));
            }
          }

          spec.addStatement("return $L.$L($L)", parent, methodName, params.toString());
        } else {
          spec.addStatement("return $T.withAppendedId(uri, id)", Clazz.CONTENT_URIS);
        }

        spec.endControlFlow();
      }
    }

    spec.beginControlFlow("default:")
        .addStatement("throw new $T(\"Unknown URI \" + uri)", IllegalArgumentException.class)
        .endControlFlow()
        .endControlFlow();

    return spec.build();
  }

  private MethodSpec getUpdateSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("update")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(int.class)
        .addParameter(Clazz.URI, "uri")
        .addParameter(Clazz.CONTENT_VALUES, "values")
        .addParameter(String.class, "where")
        .addParameter(ArrayTypeName.of(String.class), "whereArgs");

    spec.addStatement("final $T db = database.getWritableDatabase()", Clazz.SQLITE_DATABASE)
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowUpdate) {
        spec.beginControlFlow("case $L:", uri.name)
            .addStatement("$T builder = getBuilder($S)", Clazz.SELECTION_BUILDER,
                uri.parent.getSimpleName().toString());

        if (uri.contractType == UriContract.Type.INEXACT) {
          for (int i = 0; i < uri.whereColumns.length; i++) {
            String column = uri.whereColumns[i];
            int pathSegment = uri.pathSegments[i];
            spec.addStatement("builder.where(\"$L=?\", uri.getPathSegments().get($L))", column,
                pathSegment);
          }
        }
        for (String where : uri.where) {
          spec.addStatement("builder.where($S)", where);
        }
        spec.addStatement("builder.where(where, whereArgs)");

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
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else {
              error(String.format("@Where does not support parameter %s", paramType.toString()));
            }
          }

          spec.addStatement("$T wheres = $L.$L($L)", ArrayTypeName.of(String.class), parent,
              methodName, params.toString())
              .beginControlFlow("for ($T updateWhere : wheres)", String.class)
              .addStatement("builder.where(updateWhere)")
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
            VariableElement variableElement = param;
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else if (Clazz.CONTENT_VALUES.equals(ClassName.get(paramType))) {
              params.append("values");
            } else if (ClassName.get(String.class).equals(ClassName.get(paramType))) {
              params.append("builder.getSelection()");
            } else if ("java.lang.String[]".equals(variableElement.asType().toString())) {
              params.append("builder.getSelectionArgs()");
            } else {
              error(String.format("@NotifyUpdate does not support parameter %s",
                  paramType.toString()));
            }
          }

          spec.addStatement("$T notifyUris = $L.$L($L)", ArrayTypeName.of(Clazz.URI), parent,
              methodName, params.toString());
        }

        spec.addStatement("final $T count = builder.table($S)\n.update(db, values)", int.class,
            uri.table).beginControlFlow("if (count > 0)");

        if (hasNotifyUris) {
          spec.beginControlFlow("for ($T notifyUri : notifyUris)", Clazz.URI)
              .addStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
              .endControlFlow();
        } else {
          spec.addStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        spec.endControlFlow().addStatement("return count").endControlFlow();
      }
    }

    spec.beginControlFlow("default:")
        .addStatement("throw new $T(\"Unknown URI \" + uri)", IllegalArgumentException.class)
        .endControlFlow()
        .endControlFlow();

    return spec.build();
  }

  private MethodSpec getDeleteSpec() {
    MethodSpec.Builder spec = MethodSpec.methodBuilder("delete")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(int.class)
        .addParameter(Clazz.URI, "uri")
        .addParameter(String.class, "where")
        .addParameter(ArrayTypeName.of(String.class), "whereArgs")
        .addStatement("final $T db = database.getWritableDatabase()", Clazz.SQLITE_DATABASE)
        .beginControlFlow("switch(MATCHER.match(uri))");

    for (UriContract uri : uris) {
      if (uri.allowDelete) {
        spec.beginControlFlow("case $L:", uri.name)
            .addStatement("$T builder = getBuilder($S)", Clazz.SELECTION_BUILDER,
                uri.parent.getSimpleName().toString());

        if (uri.contractType == UriContract.Type.INEXACT) {
          for (int i = 0; i < uri.whereColumns.length; i++) {
            String column = uri.whereColumns[i];
            int pathSegment = uri.pathSegments[i];
            spec.addStatement("builder.where(\"$L=?\", uri.getPathSegments().get($L))", column,
                pathSegment);
          }
        }
        for (String where : uri.where) {
          spec.addStatement("builder.where($S)", where);
        }
        spec.addStatement("builder.where(where, whereArgs)");

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
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else {
              error(String.format("@Where does not support parameter %s", paramType.toString()));
            }
          }

          spec.addStatement("$T wheres = $L.$L($L)", ArrayTypeName.of(String.class), parent,
              methodName, params.toString());
          spec.beginControlFlow("for ($T deleteWhere : wheres)", String.class)
              .addStatement("builder.where(deleteWhere)")
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
            if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
              params.append("getContext()");
            } else if (Clazz.URI.equals(ClassName.get(paramType))) {
              params.append("uri");
            } else if (ClassName.get(String.class).equals(ClassName.get(paramType))) {
              params.append("builder.getSelection()");
            } else if (ArrayTypeName.get(String.class).equals(ArrayTypeName.get(paramType))) {
              params.append("builder.getSelectionArgs()");
            } else {
              error(String.format("@NotifyDelete does not support parameter %s",
                  paramType.toString()));
            }
          }

          spec.addStatement("$T notifyUris = $L.$L($L)", ArrayTypeName.of(Clazz.URI), parent,
              methodName, params.toString());
        }

        spec.addStatement("final int count = builder\n.table($S)\n.delete(db)", uri.table);

        if (hasNotifyUris) {
          spec.beginControlFlow("for ($T notifyUri : notifyUris)", Clazz.URI)
              .addStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
              .endControlFlow();
        } else {
          spec.addStatement("getContext().getContentResolver().notifyChange(uri, null)");
        }

        spec.addStatement("return count").endControlFlow();
      }
    }

    spec.beginControlFlow("default:")
        .addStatement("throw new $T(\"Unknown URI \" + uri)", IllegalArgumentException.class)
        .endControlFlow()
        .endControlFlow();

    return spec.build();
  }

  private CodeBlock getNotifyInsert(UriContract uri) {
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
      if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
        params.append("getContext()");
      } else if (Clazz.URI.equals(ClassName.get(paramType))) {
        params.append("uri");
      } else if (Clazz.CONTENT_VALUES.equals(ClassName.get(paramType))) {
        params.append("values");
      } else if (TypeName.LONG.equals(TypeName.get(paramType))) {
        params.append("id");
      } else if (ClassName.get(String.class).equals(ClassName.get(paramType))) {
        params.append("where");
      } else if (ArrayTypeName.get(String.class).equals(ArrayTypeName.get(paramType))) {
        params.append("whereArgs");
      } else {
        error(String.format("@NotifyInsert does not support parameter %s",
            paramType.toString()));
      }
    }

    return CodeBlock.builder()
        .addStatement("$T[] notifyUris = $L.$L($L)", Clazz.URI, parent, methodName,
            params.toString())
        .beginControlFlow("for ($T notifyUri : notifyUris)", Clazz.URI)
        .addStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
        .endControlFlow()
        .build();
  }

  private CodeBlock getNotifyBulkInsert(UriContract uri) {
    ExecutableElement notifyMethod = notifyBulkInsert.get(uri.path);
    if (notifyMethod == null) {
      notifyMethod = defaultNotifyBulkInsert;
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
      if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
        params.append("getContext()");
      } else if (Clazz.URI.equals(ClassName.get(paramType))) {
        params.append("uri");
      } else if (ArrayTypeName.of(Clazz.CONTENT_VALUES).equals(ArrayTypeName.get(paramType))) {
        params.append("values");
      } else if (ArrayTypeName.of(ArrayTypeName.LONG).equals(ArrayTypeName.get(paramType))) {
        params.append("ids");
      } else {
        error(String.format("@NotifyBulkInsert does not support parameter %s",
            paramType.toString()));
      }
    }

    return CodeBlock.builder()
        .addStatement("$T[] notifyUris = $L.$L($L)", Clazz.URI, parent, methodName,
            params.toString())
        .beginControlFlow("for ($T notifyUri : notifyUris)", Clazz.URI)
        .addStatement("getContext().getContentResolver().notifyChange(notifyUri, null)")
        .endControlFlow()
        .build();
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
