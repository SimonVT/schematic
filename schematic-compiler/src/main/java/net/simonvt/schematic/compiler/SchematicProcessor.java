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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
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

  private void processDatabases(RoundEnvironment env) {
    for (Element database : env.getElementsAnnotatedWith(Database.class)) {
      try {
        DatabaseWriter writer = new DatabaseWriter(processingEnv, database, outPackage);
        writer.writeJava(filer);
        writer.writeValues(filer);
      } catch (IOException e) {
        error("Unable to process " + database.asType().getKind().name());
        throw new RuntimeException(e);
      }
    }

    for (Element provider : env.getElementsAnnotatedWith(ContentProvider.class)) {
      try {
        new ContentProviderWriter(processingEnv, provider, outPackage).write(filer);
      } catch (IOException e) {
        error("Unable to process " + provider.asType().getKind().name());
        throw new RuntimeException(e);
      }
    }
  }

  private void error(String error) {
    processingEnv.getMessager().printMessage(Kind.ERROR, error);
  }
}
