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

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.Check;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;
import net.simonvt.schematic.annotation.Table;
import net.simonvt.schematic.annotation.Unique;

public class TableWriter {

  ProcessingEnvironment processingEnv;

  String name;
  boolean ifNotExists;

  Check checkConstraint;

  VariableElement table;

  TypeElement columnsClass;

  List<VariableElement> columns = new ArrayList<>();

  List<String> primaryKeys = new ArrayList<>();

  public TableWriter(ProcessingEnvironment env, VariableElement table) {
    this.processingEnv = env;
    this.table = table;
    this.name = table.getConstantValue().toString();
    Table columns = table.getAnnotation(Table.class);
    try {
      columns.value();
    } catch (MirroredTypeException e) {
      TypeMirror mirror = e.getTypeMirror();
      columnsClass = (TypeElement) env.getTypeUtils().asElement(mirror);
    }

    IfNotExists ifNotExists = table.getAnnotation(IfNotExists.class);
    this.ifNotExists = ifNotExists != null;

    List<? extends TypeMirror> interfaces = columnsClass.getInterfaces();

    checkConstraint = columnsClass.getAnnotation(Check.class);

    findColumns(columnsClass.getEnclosedElements());

    for (TypeMirror mirror : interfaces) {
      TypeElement parent = (TypeElement) env.getTypeUtils().asElement(mirror);
      findColumns(parent.getEnclosedElements());
    }
  }

  private void findColumns(List<? extends Element> elements) {
    for (Element element : elements) {
      if (!(element instanceof VariableElement)) {
        continue;
      }

      VariableElement variableElement = (VariableElement) element;

      DataType dataType = variableElement.getAnnotation(DataType.class);
      if (dataType == null) {
        continue;
      }

      String columnName = variableElement.getConstantValue().toString();

      PrimaryKey primaryKey = variableElement.getAnnotation(PrimaryKey.class);
      if (primaryKey != null) {
        primaryKeys.add(columnName);
      }

      this.columns.add(variableElement);
    }
  }

  public void createTable(TypeSpec.Builder databaseBuilder, ClassName tableClassName)
      throws IOException {
    List<ClassName> classes = new ArrayList<>();

    StringBuilder query = new StringBuilder("\"CREATE TABLE ");
    if (ifNotExists) {
      query.append("IF NOT EXISTS ");
    }
    query.append(name).append(" (");

    final int primaryKeyCount = primaryKeys.size();

    boolean first = true;
    for (VariableElement element : columns) {
      if (!first) {
        query.append(",");
      } else {
        first = false;
      }

      query.append("\"\n");

      DataType dataType = element.getAnnotation(DataType.class);

      String columnName = element.getConstantValue().toString();
      query.append(" + ").append("$T");

      classes.add(tableClassName);

      query.append(".").append(element.getSimpleName().toString()).append(" + ");
      query.append("\" ").append(dataType.value());

      NotNull notNull = element.getAnnotation(NotNull.class);
      if (notNull != null) {
        query.append(" ").append("NOT NULL");
        writeOnConflict(query, notNull.onConflict());
      }

      DefaultValue defaultValue = element.getAnnotation(DefaultValue.class);
      if (defaultValue != null) {
        query.append(" ").append("DEFAULT ").append(defaultValue.value());
      }

      PrimaryKey primary = element.getAnnotation(PrimaryKey.class);
      if (primary != null && primaryKeyCount == 1) {
        query.append(" ").append("PRIMARY KEY");
        writeOnConflict(query, primary.onConflict());
      }

      Unique unique = element.getAnnotation(Unique.class);
      if (unique != null) {
        query.append(" ").append("UNIQUE");
        writeOnConflict(query, unique.onConflict());
      }

      Check check = element.getAnnotation(Check.class);
      if (check != null) {
        writeCheckConstraint(query, check);
      }

      AutoIncrement autoIncrement = element.getAnnotation(AutoIncrement.class);
      if (autoIncrement != null) {
        if (primaryKeyCount > 1) {
          throw new IllegalArgumentException(
              "AutoIncrement is not allowed when multiple primary keys are defined");
        }

        query.append(" ").append("AUTOINCREMENT");
      }

      References references = element.getAnnotation(References.class);
      if (references != null) {
        query.append(" ")
            .append("REFERENCES ")
            .append(references.table())
            .append("(")
            .append(references.column())
            .append(")");
      }
    }

    if (primaryKeyCount > 1) {
      query.append(",\"\n + \"PRIMARY KEY (");
      first = true;
      for (String columnName : primaryKeys) {
        if (!first) {
          query.append(",");
        } else {
          first = false;
        }
        query.append(columnName);
      }
      query.append(")");
    }

    if (checkConstraint != null) {
      query.append(",\"\n + \"");
      writeCheckConstraint(query, checkConstraint);
    }

    query.append(")\"");

    FieldSpec tableSpec = FieldSpec.builder(String.class, table.getSimpleName().toString())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(query.toString(), classes.toArray())
        .build();

    databaseBuilder.addField(tableSpec);
  }

  private void writeCheckConstraint(StringBuilder query, Check check) {
    query.append(" ").append("CHECK ( ").append(check.value()).append(" )");
  }

  private static void writeOnConflict(StringBuilder query,
      ConflictResolutionType conflictResolution) {
    if (conflictResolution != ConflictResolutionType.NONE) {
      query.append(" ON CONFLICT ");
      switch (conflictResolution) {
        case ROLLBACK:
          query.append("ROLLBACK");
          break;
        case ABORT:
          query.append("ABORT");
          break;
        case FAIL:
          query.append("FAIL");
          break;
        case IGNORE:
          query.append("IGNORE");
          break;
        case REPLACE:
          query.append("REPLACE");
          break;
      }
    }
  }

  public void createValuesBuilder(Filer filer, String outPackage) throws IOException {
    String name = Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
    String valuesPackage = outPackage + ".values";
    String className = name + "ValuesBuilder";
    String qualifiedName = outPackage + ".values." + className;

    JavaFileObject jfo = filer.createSourceFile(qualifiedName);
    Writer out = jfo.openWriter();

    TypeSpec.Builder valuesBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);

    FieldSpec valuesSpec = FieldSpec.builder(Clazz.CONTENT_VALUES, "values")
        .initializer("new $T()", Clazz.CONTENT_VALUES)
        .build();
    valuesBuilder.addField(valuesSpec);

    for (VariableElement element : columns) {
      String elmName = element.getSimpleName().toString();
      elmName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, elmName);

      DataType dataType = element.getAnnotation(DataType.class);
      DataType.Type type = dataType.value();

      String column = element.getSimpleName().toString();

      switch (type) {
        case INTEGER:
          valuesBuilder.addMethod(
              makePutMethodSpec(valuesPackage, className, elmName, column, Integer.class));
          valuesBuilder.addMethod(
              makePutMethodSpec(valuesPackage, className, elmName, column, Long.class));
          break;

        case REAL:
          valuesBuilder.addMethod(
              makePutMethodSpec(valuesPackage, className, elmName, column, Float.class));
          valuesBuilder.addMethod(
              makePutMethodSpec(valuesPackage, className, elmName, column, Double.class));
          break;

        case TEXT:
          valuesBuilder.addMethod(
              makePutMethodSpec(valuesPackage, className, elmName, column, String.class));
          break;

        case BLOB:
          // TODO: What do I call here?
          break;
      }
    }

    valuesBuilder.addMethod(MethodSpec.methodBuilder("values")
        .returns(Clazz.CONTENT_VALUES)
        .addModifiers(Modifier.PUBLIC)
        .addStatement("return values")
        .build());

    JavaFile javaFile = JavaFile.builder(valuesPackage, valuesBuilder.build()).build();
    javaFile.writeTo(out);
    out.flush();
    out.close();
  }

  private MethodSpec makePutMethodSpec(String valuesPackage, String className, String elmName, String column, Type paramType) {
    return MethodSpec.methodBuilder(elmName)
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(valuesPackage, className))
        .addParameter(paramType, "value")
        .addStatement("values.put($T.$L, value)", ClassName.get(columnsClass), column)
        .addStatement("return this")
        .build();
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Kind.ERROR, error);
  }
}
