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
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.ExecOnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;
import net.simonvt.schematic.annotation.Version;

public class DatabaseWriter {

  ProcessingEnvironment processingEnv;

  Elements elementUtils;

  String className;

  String fileName;

  List<VariableElement> tables = new ArrayList<VariableElement>();
  List<VariableElement> execOnCreate = new ArrayList<VariableElement>();

  Element onUpgrade;

  private String version;

  String outPackage;

  public DatabaseWriter(ProcessingEnvironment env, Element database, String outPackage) {
    this.processingEnv = env;
    this.elementUtils = env.getElementUtils();

    this.outPackage = outPackage;

    Database db = database.getAnnotation(Database.class);
    this.className = db.className();
    this.fileName = db.fileName();

    List<? extends Element> enclosedElements = database.getEnclosedElements();
    for (Element enclosedElement : enclosedElements) {
      Table table = enclosedElement.getAnnotation(Table.class);
      if (table != null) {
        tables.add((VariableElement) enclosedElement);
      }

      OnUpgrade onUpgrade = enclosedElement.getAnnotation(OnUpgrade.class);
      if (onUpgrade != null) {
        if (this.onUpgrade != null) {
          // TODO: ERROR
        }

        this.onUpgrade = enclosedElement;
      }

      Version version = enclosedElement.getAnnotation(Version.class);
      if (version != null) {
        // TODO: Error if version already set. At end, error if no version is set
        this.version = ((VariableElement) enclosedElement).getConstantValue().toString();
      }

      ExecOnCreate execOnCreate = enclosedElement.getAnnotation(ExecOnCreate.class);
      if (execOnCreate != null) {
        this.execOnCreate.add((VariableElement) enclosedElement);
      }
    }
  }

  public void addTable(VariableElement table) {
    tables.add(table);
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
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL), version).emitEmptyLine();

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
        .endMethod();

    writer.beginConstructor(EnumSet.of(Modifier.PRIVATE), "Context", "context")
        .emitStatement("super(context, \"%s\", null, DATABASE_VERSION)", fileName)
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

    writer.endMethod().emitEmptyLine();

    writer.emitAnnotation(Override.class)
        .beginMethod("void", "onUpgrade", EnumSet.of(Modifier.PUBLIC), "SQLiteDatabase", "db",
            "int", "oldVersion", "int", "newVersion");

    if (onUpgrade != null) {
      String parent = ((TypeElement) onUpgrade.getEnclosingElement()).getQualifiedName().toString();
      String methodName = onUpgrade.getSimpleName().toString();
      writer.emitStatement("%s.%s(db, oldVersion, newVersion)", parent, methodName);
    }
    writer.endMethod();

    writer.endType().close();
  }

  private String getFileName() {
    return outPackage + "." + className;
  }

  private String getPackageName(Element type) {
    return elementUtils.getPackageOf(type).getQualifiedName().toString();
  }
}
