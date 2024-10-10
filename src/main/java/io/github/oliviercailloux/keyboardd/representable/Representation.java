package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import java.util.Objects;
import org.w3c.dom.Document;

public class Representation {
  public static Representation fromString(String string) {
    return new Representation(string, null);
  }

  public static Representation fromSvg(Document svg) {
    return new Representation(null, svg);
  }

  /** null iff svg is not */
  private final String string;

  private final Document svg;

  private Representation(String string, Document svg) {
    checkArgument((string == null) != (svg == null));
    this.string = string;
    if (svg != null) {
      this.svg = (Document) svg.cloneNode(true);
    } else {
      this.svg = null;
    }
  }

  public boolean isString() {
    return string != null;
  }

  public boolean isSvg() {
    return svg != null;
  }

  public String string() {
    checkState(string != null);
    return string;
  }

  public Document svg() {
    checkState(svg != null);
    return svg;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Representation)) {
      return false;
    }
    final Representation t2 = (Representation) o2;
    return Objects.equals(string, t2.string)
        && ((svg == null && t2.svg == null) || svg.isEqualNode(t2.svg));
  }

  @Override
  public int hashCode() {
    return Objects.hash(string, svg);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("string", string)
        .add("doc", svg == null ? "null" : DomHelper.toDebugString(svg)).toString();
  }
}
