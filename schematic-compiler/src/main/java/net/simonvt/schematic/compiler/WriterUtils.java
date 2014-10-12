package net.simonvt.schematic.compiler;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.util.EnumSet;
import javax.lang.model.element.Modifier;

public final class WriterUtils {

  private WriterUtils() {
  }

  public static void singleton(JavaWriter writer, String className, String... params)
      throws IOException {
    writer.emitField(className, "instance",
        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.VOLATILE)).emitEmptyLine();

    StringBuilder paramsBuilder = new StringBuilder();
    boolean first = true;
    int size = params.length;
    for (int i = 1; i < size; i += 2) {
      if (!first) {
        paramsBuilder.append(", ");
      } else {
        first = false;
      }

      paramsBuilder.append(params[i]);
    }

    writer.beginMethod(className, "getInstance", EnumSet.of(Modifier.PUBLIC, Modifier.STATIC),
        params)
        .beginControlFlow("if (instance == null)")
        .beginControlFlow("synchronized (" + className + ".class)")
        .beginControlFlow("if (instance == null)")
        .emitStatement("instance = new %s(%s)", className, paramsBuilder.toString())
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .emitEmptyLine()
        .emitStatement("return instance")
        .endMethod()
        .emitEmptyLine();
  }
}
