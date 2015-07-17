package net.simonvt.schematic.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

public final class WriterUtils {

  private WriterUtils() {
  }

  public static void singleton(TypeSpec.Builder typeSpec, ClassName className,
      ClassName... params) {
    FieldSpec instance = FieldSpec.builder(className, "instance", Modifier.PRIVATE, Modifier.STATIC,
        Modifier.VOLATILE).build();
    typeSpec.addField(instance);

    StringBuilder paramsBuilder = new StringBuilder();
    boolean first = true;
    int size = params.length;

    for (ClassName param : params) {
      if (!first) {
        paramsBuilder.append(", ");
      } else {
        first = false;
      }

      String simpleName = param.simpleName();
      String paramName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
      paramsBuilder.append(paramName);
    }

    MethodSpec.Builder spec = MethodSpec.methodBuilder("getInstance")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(className);

    for (ClassName param : params) {
      String simpleName = param.simpleName();
      String paramName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
      spec.addParameter(param, paramName);
    }

    spec.beginControlFlow("if (instance == null)")
        .beginControlFlow("synchronized ($T.class)", className)
        .beginControlFlow("if (instance == null)")
        .addStatement("instance = new $T($L)", className, paramsBuilder.toString())
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .addStatement("return instance");

    typeSpec.addMethod(spec.build());
  }
}
