package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.keyboardd.keyboard.RectangularKey;
import io.github.oliviercailloux.keyboardd.keyboard.RectangularKeyboard;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SvgKeyboard {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(SvgKeyboard.class);

  private static final XmlName SVG_RECT_NAME =
      XmlName.expandedName(SvgDocumentHelper.SVG_NS_URI, "rect");

  public static final URI KEYBOARDD_NS = URI.create("https://io.github.oliviercailloux.keyboardd");
  private static final String KEYBOARDD_X_KEY_NAME_LOCAL_NAME = "x-key-name";
  public static final XmlName KEYBOARDD_X_KEY_NAME =
      XmlName.expandedName(KEYBOARDD_NS, KEYBOARDD_X_KEY_NAME_LOCAL_NAME);

  private static ImmutableList<Element> getElements(Element root, XmlName name) {
    if (name.namespace().isPresent())
      return DomHelper.toElements(
          root.getElementsByTagNameNS(name.namespace().get().toString(), name.localName()));
    return DomHelper.toElements(root.getElementsByTagName(name.localName()));
  }

  /**
   * @deprecated Use JARiS.
   */
  @Deprecated()
  private static boolean hasAttribute(Element element, XmlName name) {
    return element.hasAttributeNS(name.namespace().map(URI::toString).orElse(null),
        name.localName());
  }

  /**
   * @deprecated Use JARiS.
   */
  @Deprecated()
  private static String getAttribute(Element element, XmlName name) {
    checkArgument(hasAttribute(element, name));
    return element.getAttributeNS(name.namespace().map(URI::toString).orElse(null),
        name.localName());
  }

  /**
   * @deprecated Move to JARiS.
   */
  @Deprecated()
  private static void setAttribute(Element element, XmlName name, String value) {
    element.setAttributeNS(name.namespace().map(URI::toString).orElse(null), name.localName(),
        value);
  }

  private static record LineColDivision (int n, int nbCols, int nbLines) {
    public static LineColDivision forNb(int n) {
      /*
       * Given n the nuber of representations, we want to determine suitable values for x = nb
       * columns and y = nb lines (all three non negative integer values). We want to minimize the
       * number of lines without creating overly long lines (in other words, while keeping the
       * number of columns x within reasonable range), and therefore we opt for the minimal integer
       * value y such that x ≤ 2y. With 1 ≤ y lines, we need x = roundup(n / y) columns. Thus (if 1
       * ≤ n), we want the minimal y such that roundup(n / y) ≤ 2y. Note that roundup(n / y) ≤ 2y
       * iff n ≤ 2y². Thus, we want the minimal y such that n ≤ 2y², equivalently, such that y ≥
       * sqrt(n / 2). In other words, we want y = roundup(sqrt(n / 2)), and x = roundup(n / y).
       */
      int y = (int) Math.ceil(Math.sqrt(n / 2d));
      int x = y == 0 ? 0 : (int) Math.ceil(n / (double) y);
      verify(x <= 2 * y);
      /* Check that a smaller y is not suitable. */
      verify(Math.ceil(n / (double) (y - 1)) > 2 * (y - 1));
      return new LineColDivision(n, x, y);
    }

    public ImmutableSet<PositiveSize> offsets(PositiveSize size) {
      int nbShorterLines = nbCols * nbLines - n;
      int nbFullLines = nbLines - nbShorterLines;
      double xStep = size.x() / nbCols;
      double yStep = size.y() / nbLines;
      ImmutableSet.Builder<PositiveSize> builder = ImmutableSet.builder();
      for (int col = 0; col < nbCols - 1; ++col) {
        for (int line = nbLines - 1; line >= 0; --line) {
          builder.add(new PositiveSize((col + 0.5) * xStep, (line + 0.5) * yStep));
        }
      }
      int col = nbCols - 1;
      for (int line = nbLines - 1; line > nbLines - 1 - nbFullLines; --line) {
        builder.add(new PositiveSize((col + 0.5) * xStep, (line + 0.5) * yStep));
      }
      ImmutableSet<PositiveSize> offsets = builder.build();
      verify(offsets.size() == n);
      return offsets;
    }
  }

  private static Element toSvg(SvgDocumentHelper h, Representation r) {
    if (r.isString()) {
      return h.text().setContent(r.string()).getElement();
    }
    return (Element) h.document().importNode(r.svg().getDocumentElement(), true);
  }

  /**
   * 1 unit in the given physical keyboard is rendered as 1 cm at 96 DPI (thus as 96/2.54 ≅ 38 dots)
   */
  public static SvgKeyboard zonedFrom(RectangularKeyboard physicalKeyboard) {
    final DomHelper d = DomHelper.domHelper();
    final SvgDocumentHelper h = SvgDocumentHelper.using(d);
    final Document doc = h.document();
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:kdd",
        KEYBOARDD_NS.toString());

    /*
     * Not sure the dominant baseline trick is appropriate when the baselines are not uniform (p VS
     * t), but that’ll do for now as we write capital letters.
     */
    StyleElement style = h.style().setContent("""
        rect {
          fill-opacity: 0;
          stroke: black;
          stroke-width: 1px;
        }
        text{
          text-anchor: middle;
          dominant-baseline: middle;
          font-size: small;
        }
        """);
    doc.getDocumentElement().appendChild(style.getElement());

    double dpi = 96d;
    double dotsPerCm = dpi / 2.54d;
    h.setSize(physicalKeyboard.size().mult(dotsPerCm));

    DoublePoint start = DoublePoint.zero();
    for (RectangularKey key : physicalKeyboard.keys()) {
      DoublePoint posScaled = start.plus(key.topLeftCorner()).mult(dotsPerCm);
      PositiveSize sizeScaled = key.size().mult(dotsPerCm);
      RectangleElement rect =
          h.rectangle().setRounding(10d).setStart(posScaled).setSize(sizeScaled);
      setAttribute(rect.getElement(), KEYBOARDD_X_KEY_NAME, key.xKeyName());
      doc.getDocumentElement().appendChild(rect.getElement());
    }

    // return new SvgKeyboard(doc).withRepresentations(k ->
    // ImmutableList.of(Representation.fromString(k)));
    return new SvgKeyboard(doc);
  }

  public static SvgKeyboard using(Document doc) {
    return new SvgKeyboard(doc);
  }

  private final Document doc;

  private SvgKeyboard(Document doc) {
    this.doc = doc;
  }

  public Document document() {
    return doc;
  }

  public ImmutableMap<RectangleElement, String> keyNameByZone() {
    ImmutableMap.Builder<RectangleElement, String> reprsBuilder = ImmutableMap.builder();
    for (Element rect : getElements(doc.getDocumentElement(), SVG_RECT_NAME)) {
      if (!hasAttribute(rect, KEYBOARDD_X_KEY_NAME))
        continue;
      String xKeyName = getAttribute(rect, KEYBOARDD_X_KEY_NAME);
      reprsBuilder.put(RectangleElement.using(rect), xKeyName);
    }
    return reprsBuilder.build();
  }

  /**
   * Adds representations to the zones found in this document, according to the given function.
   * 
   * @param representationsByXKeyName the respective representations to add to the zones.
   * @return this object
   */
  public SvgKeyboard withRepresentations(
      Function<String, ? extends List<Representation>> representationsByXKeyName) {
    // Thanks to https://stackoverflow.com/questions/5226852/cloning-dom-document-object . TODO
    // document in Jaris?
    // DOMResult result = new DOMResult();
    // XmlTransformer.usingFoundFactory().usingEmptyStylesheet().transform(new DOMSource(doc),
    // result);
    // Document d = (Document) result.getNode();
    SvgDocumentHelper h = SvgDocumentHelper.using(doc);
    ImmutableMap<RectangleElement, String> keyNameByZone = keyNameByZone();
    for (RectangleElement zone : keyNameByZone.keySet()) {
      String xKeyName = keyNameByZone.get(zone);
      List<Representation> reprs = representationsByXKeyName.apply(xKeyName);
      DoublePoint start = zone.getStart();
      PositiveSize startOffset = PositiveSize.between(DoublePoint.zero(), start);
      LineColDivision div = LineColDivision.forNb(reprs.size());
      ImmutableSet<PositiveSize> offsets = div.offsets(zone.getSize());
      UnmodifiableIterator<PositiveSize> offsetsIt = offsets.iterator();
      for (Representation r : reprs) {
        PositiveSize offset = offsetsIt.next();
        Element svgRepr = toSvg(h, r);
        Element g = h.g().translate(startOffset.plus(offset)).getElement();
        g.appendChild(svgRepr);
        Node prev = zone.getElement().getNextSibling();
        zone.getElement().getParentNode().insertBefore(g, prev);
      }
      verify(!offsetsIt.hasNext());
    }
    return this;
  }
}
