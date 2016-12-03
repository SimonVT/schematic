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
import java.util.Arrays;
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
import net.simonvt.schematic.annotation.Constraints;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.ForeignKeyConstraint;
import net.simonvt.schematic.annotation.IfNotExists;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.PrimaryKeyConstraint;
import net.simonvt.schematic.annotation.References;
import net.simonvt.schematic.annotation.Table;
import net.simonvt.schematic.annotation.Unique;
import net.simonvt.schematic.annotation.UniqueConstraint;

public class TableWriter {

  ProcessingEnvironment processingEnv;

  String name;
  boolean ifNotExists;

  private List<UniqueConstraint> uniqueConstraints = new ArrayList<>();
  private List<Check> checkConstraints = new ArrayList<>();
  private List<ForeignKeyConstraint> foreignKeyConstraints = new ArrayList<>();
  private PrimaryKeyConstraint tableLevelPrimaryKey;

  private PrimaryKey primaryKey;

  VariableElement table;
  ClassName tableClassName;

  TypeElement columnsClass;

  List<VariableElement> columns = new ArrayList<>();

  public TableWriter(ProcessingEnvironment env, VariableElement table, ClassName tableClassName) {
    this.processingEnv = env;
    this.table = table;
    this.name = table.getConstantValue().toString();
    this.tableClassName = tableClassName;

    Table columns = table.getAnnotation(Table.class);
    try {
      columns.value();
    } catch (MirroredTypeException e) {
      TypeMirror mirror = e.getTypeMirror();
      columnsClass = (TypeElement) env.getTypeUtils().asElement(mirror);
    }

    IfNotExists ifNotExists = table.getAnnotation(IfNotExists.class);
    this.ifNotExists = ifNotExists != null;

    fillEntireTableConstrains(columnsClass);

    List<? extends TypeMirror> interfaces = columnsClass.getInterfaces();

    findColumns(columnsClass.getEnclosedElements());

    for (TypeMirror mirror : interfaces) {
      TypeElement parent = (TypeElement) env.getTypeUtils().asElement(mirror);
      findColumns(parent.getEnclosedElements());
    }
  }

  private void fillEntireTableConstrains(TypeElement columnsClass) {
    Constraints entireTableConstraints = columnsClass.getAnnotation(Constraints.class);
    if (entireTableConstraints != null) {
      for (UniqueConstraint uniqueConstraint : entireTableConstraints.unique()) {
        uniqueConstraints.add(uniqueConstraint);
      }

      for (Check checkConstraint : entireTableConstraints.check()) {
        checkConstraints.add(checkConstraint);
      }

      for (ForeignKeyConstraint foreignKey : entireTableConstraints.foreignKey()) {
        foreignKeyConstraints.add(foreignKey);
      }
    }

    UniqueConstraint uniqueConstraint = columnsClass.getAnnotation(UniqueConstraint.class);
    if (uniqueConstraint != null) {
      uniqueConstraints.add(uniqueConstraint);
    }

    Check checkConstraint = columnsClass.getAnnotation(Check.class);
    if (checkConstraint != null) {
      checkConstraints.add(checkConstraint);
    }

    ForeignKeyConstraint foreignKey = columnsClass.getAnnotation(ForeignKeyConstraint.class);
    if (foreignKey != null) {
      foreignKeyConstraints.add(foreignKey);
    }

    tableLevelPrimaryKey = columnsClass.getAnnotation(PrimaryKeyConstraint.class);
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
        if (this.primaryKey != null) {
          error(String.format("Only one primary key can be defined for table. " +
              "Use PrimaryKey or PrimaryKeyConstraint. Not both. " +
              "Found in %s", tableClassName));
        }
        if (tableLevelPrimaryKey != null) {
          error(String.format(
              "Only one primary key can be defined for table. " +
                  "Use PrimaryKey or PrimaryKeyConstraint. Not both. " +
                  "Found in %s", tableClassName));
        }

        this.primaryKey = primaryKey;
      }

      this.columns.add(variableElement);
    }
  }

  public void createTable(TypeSpec.Builder databaseBuilder)
      throws IOException {
    List<ClassName> classes = new ArrayList<>();

    StringBuilder query = new StringBuilder("\"CREATE TABLE ");
    if (ifNotExists) {
      query.append("IF NOT EXISTS ");
    }
    query.append(name).append(" (");

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
      if (primary != null) {
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
        if (tableLevelPrimaryKey != null) {
          error(String.format(
              "AutoIncrement is not allowed when multiple primary keys are defined. "
                  + "Found in %s", tableClassName));
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

    if (tableLevelPrimaryKey != null) {
      writePrimaryKey(query,
              Arrays.asList(tableLevelPrimaryKey.columns()),
              tableLevelPrimaryKey.name(),
              tableLevelPrimaryKey.onConflict());
    }

    for (Check checkConstraint : checkConstraints) {
      query.append( ",\"\n + \"");
      writeCheckConstraint(query, checkConstraint);
    }

    if (!uniqueConstraints.isEmpty()) {
      for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
        writeUniqueConstraint(query, uniqueConstraint);
      }
    }

    if (!foreignKeyConstraints.isEmpty()) {
      for (ForeignKeyConstraint foreignKey : foreignKeyConstraints) {
        writeForeignKeyConstraint(query, foreignKey);
      }
    }

    query.append(")\"");

    FieldSpec tableSpec = FieldSpec.builder(String.class, table.getSimpleName().toString())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .initializer(query.toString(), classes.toArray())
        .build();

    databaseBuilder.addField(tableSpec);
  }

  private static void writeForeignKeyConstraint(StringBuilder query, ForeignKeyConstraint foreignKey) {
    query.append(",\"\n + \"");
    if (foreignKey.name().length() > 0) {
      query.append(' ').append("CONSTRAINT ").append(foreignKey.name());
    }
    query.append(' ').append("FOREIGN KEY").append(" (");
    for (int i = 0; i < foreignKey.columns().length; i++) {
      if (i != 0) {
        query.append(", ");
      }
      query.append(foreignKey.columns()[i]);
    }
    query.append(") ").append("REFERENCES").append(' ')
            .append(foreignKey.referencedTable())
            .append(" (");
    for (int i = 0; i < foreignKey.referencedColumns().length; i++) {
      if (i != 0) {
        query.append(", ");
      }
      query.append(foreignKey.referencedColumns()[i]);
    }
    query.append(')');

  }

  private static void writePrimaryOrUniqueConstraint(
          StringBuilder query,
          String constraintType,
          List<String> columnNames,
          String name,
          ConflictResolutionType resolutionType) {
    query.append(",\"\n + \"");
    if (name.length() > 0) {
      query.append(' ').append("CONSTRAINT ").append(name);
    }
    query.append(' ').append(constraintType).append(" ( ");
    int size = columnNames.size();
    for (int i = 0; i < size; i++) {
      if (i != 0)  query.append(", ");
      query.append(columnNames.get(i));
    }
    query.append(" )");
    writeOnConflict(query,resolutionType);
  }

  private static void writeUniqueConstraint(StringBuilder query, UniqueConstraint uniqueConstraint) {
    writePrimaryOrUniqueConstraint(query,"UNIQUE", Arrays.asList(uniqueConstraint.columns()),
            uniqueConstraint.name(), uniqueConstraint.onConflict());
  }

  private static void writePrimaryKey(
          StringBuilder query,
          List<String> columnNames,
          String constraintName,
          ConflictResolutionType resolutionType) {
    writePrimaryOrUniqueConstraint(query,
            "PRIMARY KEY", columnNames, constraintName, resolutionType);
  }

  private static void writeCheckConstraint(StringBuilder query, Check check) {
    String name = check.name();
    if (name.length() > 0) {
      query.append(' ').append("CONSTRAINT ").append(name);
    }

    query.append(' ').append("CHECK ( ").append(check.value()).append(" )");
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
