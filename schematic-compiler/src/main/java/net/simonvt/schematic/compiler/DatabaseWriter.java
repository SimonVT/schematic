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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

public class DatabaseWriter {

  ProcessingEnvironment processingEnv;

  Elements elementUtils;

  String className;

  String fileName;

  List<VariableElement> tables = new ArrayList<VariableElement>();
  List<VariableElement> execOnCreate = new ArrayList<VariableElement>();

  ExecutableElement onCreate;

  ExecutableElement onUpgrade;

  int version;

  String outPackage;

  public DatabaseWriter(ProcessingEnvironment env, Element database, String outPackage) {
    this.processingEnv = env;
    this.elementUtils = env.getElementUtils();

    this.outPackage = outPackage;

    String databaseSchematicName = database.getSimpleName().toString();

    Database db = database.getAnnotation(Database.class);
    this.version = db.version();

    this.className = db.className();
    if (className.trim().isEmpty()) {
      this.className = databaseSchematicName;
    }

    this.fileName = db.fileName();
    if (fileName.trim().isEmpty()) {
      this.fileName =
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, databaseSchematicName) + ".db";
    }

    List<? extends Element> enclosedElements = database.getEnclosedElements();
    for (Element enclosedElement : enclosedElements) {
      Table table = enclosedElement.getAnnotation(Table.class);
      if (table != null) {
        tables.add((VariableElement) enclosedElement);
      }

      OnCreate onCreate = enclosedElement.getAnnotation(OnCreate.class);
      if (onCreate != null) {
        if (this.onCreate != null) {
          error("Multiple OnCreate annotations found in " + database.getSimpleName().toString());
        }

        this.onCreate = (ExecutableElement) enclosedElement;
      }

      OnUpgrade onUpgrade = enclosedElement.getAnnotation(OnUpgrade.class);
      if (onUpgrade != null) {
        if (this.onUpgrade != null) {
          error("Multiple OnUpgrade annotations found in " + database.getSimpleName().toString());
        }

        this.onUpgrade = (ExecutableElement) enclosedElement;
      }

      ExecOnCreate execOnCreate = enclosedElement.getAnnotation(ExecOnCreate.class);
      if (execOnCreate != null) {
        this.execOnCreate.add((VariableElement) enclosedElement);
      }
    }
  }

  public void writeJava(Filer filer) throws IOException {
    JavaFileObject jfo = filer.createSourceFile(getFileName());
    Writer out = jfo.openWriter();
    JavaWriter writer = new JavaWriter(out);
    writer.emitPackage(outPackage);

    writer.emitImports("android.content.Context")
        .emitImports("android.database.sqlite.SQLiteOpenHelper")
        .emitImports("android.database.sqlite.SQLiteDatabase");

    for (VariableElement table : tables) {
      TypeElement tableClass = null;
      Table columns = table.getAnnotation(Table.class);
      try {
        columns.value();
      } catch (MirroredTypeException e) {
        TypeMirror mirror = e.getTypeMirror();
        tableClass = (TypeElement) processingEnv.getTypeUtils().asElement(mirror);
      }

      writer.emitImports(tableClass.getQualifiedName().toString());
    }

    writer.emitEmptyLine();

    writer.beginType(className, "class", EnumSet.of(Modifier.PUBLIC), "SQLiteOpenHelper")
        .emitEmptyLine();

    writer.emitField("int", "DATABASE_VERSION",
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL), String.valueOf(version))
        .emitEmptyLine();

    for (VariableElement table : tables) {
      TableWriter tableWriter = new TableWriter(processingEnv, table);
      tableWriter.createTable(writer);
      writer.emitEmptyLine();
    }

    writer.emitField(className, "instance",
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE)).emitEmptyLine();

    writer.beginMethod(className, "getInstance", EnumSet.of(Modifier.PUBLIC, Modifier.STATIC),
        "Context", "context")
        .beginControlFlow("if (instance == null)")
        .beginControlFlow("synchronized (" + className + ".class)")
        .beginControlFlow("if (instance == null)")
        .emitStatement("instance = new %s(context)", className)
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .emitEmptyLine()
        .emitStatement("return instance")
        .endMethod()
        .emitEmptyLine();

    writer.emitField("Context", "context", EnumSet.of(Modifier.PRIVATE));

    writer.beginConstructor(EnumSet.of(Modifier.PRIVATE), "Context", "context")
        .emitStatement("super(context, \"%s\", null, DATABASE_VERSION)", fileName)
        .emitStatement("this.context = context")
        .endConstructor()
        .emitEmptyLine();

    writer.emitAnnotation(Override.class)
        .beginMethod("void", "onCreate", EnumSet.of(Modifier.PUBLIC), "SQLiteDatabase", "db");

    for (VariableElement table : tables) {
      writer.emitStatement("db.execSQL(%s)", table.getSimpleName().toString());
    }

    for (VariableElement exec : execOnCreate) {
      String parent = ((TypeElement) exec.getEnclosingElement()).getQualifiedName().toString();
      String variableName = exec.getSimpleName().toString();

      writer.emitStatement("db.execSQL(%s.%s)", parent, variableName);
    }

    if (onCreate != null) {
      List<? extends VariableElement> parameters = onCreate.getParameters();
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
          params.append("context");
        }
        if ("android.database.sqlite.SQLiteDatabase".equals(typeAsString)) {
          params.append("db");
        }
      }

      String parent = ((TypeElement) onCreate.getEnclosingElement()).getQualifiedName().toString();
      String methodName = onCreate.getSimpleName().toString();
      writer.emitStatement("%s.%s(%s)", parent, methodName, params.toString());
    }

    writer.endMethod().emitEmptyLine();

    writer.emitAnnotation(Override.class)
        .beginMethod("void", "onUpgrade", EnumSet.of(Modifier.PUBLIC), "SQLiteDatabase", "db",
            "int", "oldVersion", "int", "newVersion");

    if (onUpgrade != null) {
      List<? extends VariableElement> parameters = onUpgrade.getParameters();
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
          params.append("context");
        }
        if ("android.database.sqlite.SQLiteDatabase".equals(typeAsString)) {
          params.append("db");
        }
        if ("int".equals(typeAsString)) {
          String name = param.getSimpleName().toString();
          if ("oldVersion".equals(name)) {
            params.append("oldVersion");
          } else if ("newVersion".equals(name)) {
            params.append("newVersion");
          } else {
            error("Unknown int parameter: " + name);
          }
        }
      }

      String parent = ((TypeElement) onUpgrade.getEnclosingElement()).getQualifiedName().toString();
      String methodName = onUpgrade.getSimpleName().toString();
      writer.emitStatement("%s.%s(%s)", parent, methodName, params.toString());
    }
    writer.endMethod();

    writer.endType().close();
  }

  private String getFileName() {
    return outPackage + "." + className;
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
  }
}
