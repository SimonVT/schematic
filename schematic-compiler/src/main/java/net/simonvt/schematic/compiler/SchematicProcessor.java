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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.Database;

@SupportedOptions({
    SchematicProcessor.OPTION_PACKAGE
})
public class SchematicProcessor extends AbstractProcessor {

  private static final String DEFAULT_OUT_PACKAGE = "net.simonvt.schematic.provider";

  static final String OPTION_PACKAGE = "schematicOutPackage";

  private static final List<Class<? extends Annotation>> ANNOTATIONS = Arrays.asList(//
      Database.class, //
      ContentProvider.class);

  private Elements elementUtils;
  private Types typeUtils;
  private Filer filer;

  private String outPackage;

  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);

    elementUtils = env.getElementUtils();
    typeUtils = env.getTypeUtils();
    filer = env.getFiler();

    Map<String, String> options = env.getOptions();
    if (options.containsKey(OPTION_PACKAGE)) {
      outPackage = options.get(OPTION_PACKAGE);
    } else {
      outPackage = DEFAULT_OUT_PACKAGE;
    }
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> supportedTypes = new LinkedHashSet<String>();

    for (Class<? extends Annotation> annotation : ANNOTATIONS) {
      supportedTypes.add(annotation.getCanonicalName());
    }

    return supportedTypes;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    processDatabases(env);
    return true;
  }

  static class DatabaseItem {
    TypeElement database;
    String name;

    DatabaseItem(TypeElement database, String name) {
      this.database = database;
      this.name = name;
    }
  }

  private void processDatabases(RoundEnvironment env) {
    List<DatabaseItem> databases = new ArrayList<DatabaseItem>();

    for (Element database : env.getElementsAnnotatedWith(Database.class)) {
      Database annotation = database.getAnnotation(Database.class);
      try {
        databases.add(new DatabaseItem((TypeElement) database, annotation.className()));
        new DatabaseWriter(processingEnv, database, outPackage).writeJava(filer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    for (Element provider : env.getElementsAnnotatedWith(ContentProvider.class)) {
      try {
        new ContentProviderWriter(processingEnv, provider, outPackage).write(filer);
      } catch (IOException e) {
        e.printStackTrace();
        error("Unable to process " + provider.asType().getKind().name());
      }
    }
  }

  private void writeSchematicInternal(List<DatabaseItem> databases) {
    try {
      JavaFileObject jfo = filer.createSourceFile("net.simonvt.schematic.Schematic");
      Writer out = jfo.openWriter();
      JavaWriter writer = new JavaWriter(out);

      writer.emitPackage("net.simonvt.schematic");

      writer.emitImports("android.content.Context")
          .emitImports("android.database.sqlite.SQLiteOpenHelper")
          .emitImports("java.util.HashMap")
          .emitImports("java.util.Map");

      writer.beginType("Schematic", "class", EnumSet.of(Modifier.FINAL));

      writer.emitField("Map<String, String>", "DATABASE_IMPLS",
          EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL),
          "new HashMap<String, String>()");

      writer.beginInitializer(true);
      for (DatabaseItem item : databases) {
        String definitionName = elementUtils.getBinaryName(item.database).toString();
        String implName = outPackage + item.name;
        writer.emitStatement("DATABASE_IMPLS.put(%s, %s)", definitionName, implName);
      }
      writer.endInitializer().emitEmptyLine();

      writer.emitField("Map<Class, SQLiteOpenHelper>", "DATABASES",
          EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL),
          "new HashMap<Class, Class>()").emitEmptyLine();

      writer.beginMethod("SQLiteOpenHelper", "getDatabase", EnumSet.of(Modifier.FINAL), "Context",
          "context", "Class", "database");
      writer.emitStatement("SQLiteOpenHelper openHelper = DATABASES.get(database)");
      writer.beginControlFlow("if (openHelper != null)")
          .emitStatement("return openHelper;")
          .endControlFlow()
          .emitEmptyLine();

      writer.emitStatement("String databaseName = database.getSimpleName()")
          .emitStatement("String implName = DATABASE_IMPLS.get(databaseName)")
          .emitStatement("Class implClass = Class.forName(implName)")
          .emitStatement("Method getInstance = implClass.getMethod(\"getInstance\", Context.class)")
          .emitStatement("openHelper = (SQLiteOpenHelper) getInstance.invoke(null, context)")
          .emitEmptyLine()
          .emitStatement("DATABASES.put(database, openHelper)")
          .emitEmptyLine()
          .emitStatement("return openHelper");

      writer.endMethod();

      writer.endType();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Kind.ERROR, error);
  }
}
