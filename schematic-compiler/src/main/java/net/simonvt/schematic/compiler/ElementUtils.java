package net.simonvt.schematic.compiler;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public final class ElementUtils {

  private ElementUtils() {
  }

  public static String getFullyQualified(Element element) {
    String parent = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
    String elm = element.getSimpleName().toString();
    return parent + "." + elm;
  }
}
