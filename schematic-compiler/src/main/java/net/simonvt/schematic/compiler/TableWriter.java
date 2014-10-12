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
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
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
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.References;
import net.simonvt.schematic.annotation.Table;
import net.simonvt.schematic.annotation.Unique;

public class TableWriter {

  ProcessingEnvironment processingEnv;

  String name;

  VariableElement table;

  TypeElement columnsClass;

  List<VariableElement> columns = new ArrayList<>();

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

    List<? extends TypeMirror> interfaces = columnsClass.getInterfaces();

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

      DataType dataType = element.getAnnotation(DataType.class);
      if (dataType == null) {
        continue;
      }

      this.columns.add((VariableElement) element);
    }
  }

  public void createTable(JavaWriter writer) throws IOException {
    StringBuilder query = new StringBuilder("\"CREATE TABLE " + name + " (");

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
      query.append(" + ")
          .append(columnsClass.getSimpleName().toString())
          .append(".")
          .append(element.getSimpleName().toString())
          .append(" + ");
      query.append("\" ").append(dataType.value());

      NotNull notNull = element.getAnnotation(NotNull.class);
      if (notNull != null) {
        query.append(" ").append("NOT NULL");
      }

      DefaultValue defaultValue = element.getAnnotation(DefaultValue.class);
      if (defaultValue != null) {
        query.append(" ").append("DEFAULT ").append(defaultValue.value());
      }

      PrimaryKey primary = element.getAnnotation(PrimaryKey.class);
      if (primary != null) {
        query.append(" ").append("PRIMARY KEY");
      }

      Unique unique = element.getAnnotation(Unique.class);
      if (unique != null) {
        query.append(" ").append("UNIQUE");
      }

      AutoIncrement autoIncrement = element.getAnnotation(AutoIncrement.class);
      if (autoIncrement != null) {
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

    query.append(")\"");

    writer.emitField("String", table.getSimpleName().toString(),
        EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL), query.toString());

    //writer.emitStatement("db.execSQL(\"%s\")", query.toString());
  }

  public void createValuesBuilder(Filer filer, String outPackage) {
    try {
      String name = Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
      String className = name + "ValuesBuilder";
      String qualifiedName = outPackage + ".values." + className;

      Writer out = null;
      JavaFileObject jfo = filer.createSourceFile(qualifiedName);
      out = jfo.openWriter();

      JavaWriter writer = new JavaWriter(out);

      writer.emitPackage(outPackage + ".values");

      writer.emitImports("android.content.ContentValues");
      writer.emitImports(columnsClass.getQualifiedName().toString());

      writer.emitEmptyLine();

      writer.beginType(className, "class", EnumSet.of(Modifier.PUBLIC)).emitEmptyLine();

      writer.emitField("ContentValues", "values").emitEmptyLine();

      writer.beginConstructor(EnumSet.of(Modifier.PUBLIC))
          .emitStatement("values = new ContentValues()")
          .endConstructor()
          .emitEmptyLine();

      for (VariableElement element : columns) {
        String elmName = element.getSimpleName().toString();
        elmName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, elmName);
        String colName = element.getConstantValue().toString();

        DataType dataType = element.getAnnotation(DataType.class);
        DataType.Type type = dataType.value();

        String colQualified =
            columnsClass.getSimpleName().toString() + "." + element.getSimpleName().toString();

        switch (type) {
          case INTEGER:
            writer.beginMethod(className, elmName, EnumSet.of(Modifier.PUBLIC), "int", "value")
                .emitStatement("values.put(%s, value)", colQualified)
                .emitStatement("return this")
                .endMethod()
                .emitEmptyLine();

            writer.beginMethod(className, elmName, EnumSet.of(Modifier.PUBLIC), "long", "value")
                .emitStatement("values.put(%s, value)", colQualified)
                .emitStatement("return this")
                .endMethod()
                .emitEmptyLine();
            break;

          case REAL:
            writer.beginMethod(className, elmName, EnumSet.of(Modifier.PUBLIC), "float", "value")
                .emitStatement("values.put(%s, value)", colQualified)
                .emitStatement("return this")
                .endMethod()
                .emitEmptyLine();
            break;

          case TEXT:
            writer.beginMethod(className, elmName, EnumSet.of(Modifier.PUBLIC), "String", "value")
                .emitStatement("values.put(%s, value)", colQualified)
                .emitStatement("return this")
                .endMethod()
                .emitEmptyLine();
            break;

          case BLOB:
            // TODO: What do I call here?
            break;
        }
      }

      writer.beginMethod("ContentValues", "values", EnumSet.of(Modifier.PUBLIC))
          .emitStatement("return values")
          .endMethod();

      writer.endType().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Kind.ERROR, error);
  }
}
