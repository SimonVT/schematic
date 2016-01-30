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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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
import net.simonvt.schematic.annotation.OnConfigure;
import net.simonvt.schematic.annotation.OnCreate;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

public class DatabaseWriter {

  ProcessingEnvironment processingEnv;

  Elements elementUtils;

  Element database;

  String className;

  ClassName clazzName;

  String fileName;

  List<VariableElement> tables = new ArrayList<VariableElement>();

  List<VariableElement> execOnCreate = new ArrayList<VariableElement>();

  ExecutableElement onCreate;

  ExecutableElement onUpgrade;

  ExecutableElement onConfigure;

  int version;

  String outPackage;

  public DatabaseWriter(ProcessingEnvironment env, Elements elements, Element database) {
    this.processingEnv = env;
    this.elementUtils = env.getElementUtils();

    this.database = database;

    String databaseSchematicName = database.getSimpleName().toString();

    Database db = database.getAnnotation(Database.class);
    this.version = db.version();

    this.className = db.className();
    if (className.trim().isEmpty()) {
      this.className = databaseSchematicName;
    }

    this.outPackage = db.packageName();
    if (outPackage.trim().isEmpty()) {
      this.outPackage = elements.getPackageOf(database).getQualifiedName() + ".generated";
    }

    this.fileName = db.fileName();
    if (fileName.trim().isEmpty()) {
      this.fileName =
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, databaseSchematicName) + ".db";
    }

    clazzName = ClassName.get(outPackage, className);

    findAnnotations(database);
  }

  private void findAnnotations(Element element) {
    List<? extends Element> enclosedElements = element.getEnclosedElements();

    for (Element enclosedElement : enclosedElements) {
      findAnnotations(enclosedElement);

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

      OnConfigure onConfigure = enclosedElement.getAnnotation(OnConfigure.class);
      if (onConfigure != null) {
        if (this.onConfigure != null) {
          error("Multiple OnConfigure annotations found in " + database.getSimpleName().toString());
        }

        this.onConfigure = (ExecutableElement) enclosedElement;
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

    TypeSpec.Builder databaseBuilder = TypeSpec.classBuilder(className)
        .superclass(Clazz.SQLITE_OPEN_HELPER)
        .addModifiers(Modifier.PUBLIC);

    FieldSpec versionSpec =
        FieldSpec.builder(int.class, "DATABASE_VERSION", Modifier.PRIVATE, Modifier.STATIC,
            Modifier.FINAL).initializer("$L", version).build();
    databaseBuilder.addField(versionSpec);

    for (VariableElement table : tables) {
      TypeElement tableClass = null;
      Table columns = table.getAnnotation(Table.class);
      try {
        columns.value();
      } catch (MirroredTypeException e) {
        TypeMirror mirror = e.getTypeMirror();
        tableClass = (TypeElement) processingEnv.getTypeUtils().asElement(mirror);
      }

      ClassName tableClassName = ClassName.get(tableClass);

      TableWriter tableWriter = new TableWriter(processingEnv, table);
      tableWriter.createTable(databaseBuilder, tableClassName);
      tableWriter.createValuesBuilder(filer, outPackage);
    }

    WriterUtils.singleton(databaseBuilder, clazzName, Clazz.CONTEXT);

    databaseBuilder.addField(Clazz.CONTEXT, "context", Modifier.PRIVATE);

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(Clazz.CONTEXT, "context")
        .addStatement("super(context.getApplicationContext(), $S, null, DATABASE_VERSION)", fileName)
        .addStatement("this.context = context.getApplicationContext()")
        .build();
    databaseBuilder.addMethod(constructor);

    databaseBuilder.addMethod(getOnCreateSpec());
    databaseBuilder.addMethod(getOnUpgradeSpec());

    if (onConfigure != null) {
      databaseBuilder.addMethod(getOnConfigureSpec());
    }

    JavaFile javaFile = JavaFile.builder(outPackage, databaseBuilder.build()).build();
    javaFile.writeTo(out);
    out.flush();
    out.close();
  }

  private MethodSpec getOnCreateSpec() {
    MethodSpec.Builder onCreateBuilder = MethodSpec.methodBuilder("onCreate")
        .returns(void.class)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Clazz.SQLITE_DATABASE, "db");

    for (VariableElement table : tables) {
      onCreateBuilder.addStatement("db.execSQL($L)", table.getSimpleName().toString());
    }

    for (VariableElement exec : execOnCreate) {
      String variableName = exec.getSimpleName().toString();
      onCreateBuilder.addStatement("db.execSQL($T.$L)", exec.getEnclosingElement(), variableName);
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
        if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
          params.append("context");
        }
        if (Clazz.SQLITE_DATABASE.equals(ClassName.get(paramType))) {
          params.append("db");
        }
      }

      String methodName = onCreate.getSimpleName().toString();
      onCreateBuilder.addStatement("$T.$L($L)", onCreate.getEnclosingElement(), methodName,
          params.toString());
    }

    return onCreateBuilder.build();
  }

  private MethodSpec getOnUpgradeSpec() {
    MethodSpec.Builder onUpgradeBuilder = MethodSpec.methodBuilder("onUpgrade")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Clazz.SQLITE_DATABASE, "db")
        .addParameter(int.class, "oldVersion")
        .addParameter(int.class, "newVersion");

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

        if (Clazz.CONTEXT.equals(ClassName.get(paramType))) {
          params.append("context");
        }
        if (Clazz.SQLITE_DATABASE.equals(ClassName.get(paramType))) {
          params.append("db");
        }
        if (TypeName.get(int.class).equals(TypeName.get(paramType))) {
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
      onUpgradeBuilder.addStatement("$L.$L($L)", parent, methodName, params.toString());
    }

    return onUpgradeBuilder.build();
  }

  private MethodSpec getOnConfigureSpec() {
    MethodSpec.Builder onConfigureBuilder = MethodSpec.methodBuilder("onConfigure")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Clazz.SQLITE_DATABASE, "db");

    List<? extends VariableElement> parameters = onConfigure.getParameters();
    StringBuilder params = new StringBuilder();
    boolean first = true;
    for (VariableElement param : parameters) {
      if (first) {
        first = false;
      } else {
        params.append(", ");
      }
      TypeMirror paramType = param.asType();
      if (Clazz.SQLITE_DATABASE.equals(ClassName.get(paramType))) {
        params.append("db");
      } else {
        throw new IllegalArgumentException(
            "OnConfigure does not support parameter " + paramType.toString());
      }
    }

    String parent = ((TypeElement) onConfigure.getEnclosingElement()).getQualifiedName().toString();
    String methodName = onConfigure.getSimpleName().toString();
    onConfigureBuilder.addStatement("$L.$L($L)", parent, methodName, params.toString());

    return onConfigureBuilder.build();
  }

  public void writeValues(Filer filer) throws IOException {
    JavaFileObject jfo = filer.createSourceFile(getValuesFileName());
    Writer out = jfo.openWriter();

    final String valuesPackage = outPackage + ".values";

    TypeSpec.Builder spec =
        TypeSpec.classBuilder(className + "Values").addModifiers(Modifier.PUBLIC);

    for (VariableElement table : tables) {
      Table tableAnnotation = table.getAnnotation(Table.class);
      String tableName = table.getConstantValue().toString();
      tableName = Character.toUpperCase(tableName.charAt(0)) + tableName.substring(1);

      String methodName = "for" + tableName;
      String valuesName = tableName + "ValuesBuilder";
      ClassName builderClass = ClassName.get(valuesPackage, valuesName);

      MethodSpec builderSpec = MethodSpec.methodBuilder(methodName)
          .addModifiers(Modifier.PUBLIC)
          .returns(builderClass)
          .addStatement("return new $T()", builderClass)
          .build();

      spec.addMethod(builderSpec);
    }

    JavaFile javaFile = JavaFile.builder(valuesPackage, spec.build()).build();
    javaFile.writeTo(out);
    out.flush();
    out.close();
  }

  private String getFileName() {
    return outPackage + "." + className;
  }

  private String getValuesFileName() {
    return outPackage + ".values." + className + "Values";
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
  }
}
