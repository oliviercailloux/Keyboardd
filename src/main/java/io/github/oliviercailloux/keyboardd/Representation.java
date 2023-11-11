package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Representation {
  public static Representation fromString(String string) {
    return new Representation(string, null);
  }

  public static Representation fromSvg(Element svg) {
    return new Representation(null, svg);
  }

  /** null iff svg is not */
  private final String string;

  private final Element svg;

  private Representation(String string, Element svg) {
    checkArgument((string == null) != (svg == null));
    this.string = string;
    if (svg != null)
      this.svg = (Element) svg.cloneNode(true);
    else
      this.svg = null;
  }

  public String string() {
    return string;
  }

  public Element svg() {
    return svg;
  }
}
