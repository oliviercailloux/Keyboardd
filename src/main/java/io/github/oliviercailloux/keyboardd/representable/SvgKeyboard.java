package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import io.github.oliviercailloux.svgb.SvgHelper;
import io.github.oliviercailloux.svgb.TextElement;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    if (name.namespace().isPresent()) {
      return DomHelper.toElements(
          root.getElementsByTagNameNS(name.namespace().get().toString(), name.localName()));
    }
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

    public ImmutableSet<PositiveSize> offsetsToCorners(PositiveSize size) {
      return offsets(size, 0d);
    }

    public ImmutableSet<PositiveSize> offsetsToMiddle(PositiveSize size) {
      return offsets(size, 0.5d);
    }

    private ImmutableSet<PositiveSize> offsets(PositiveSize size, double additionalColFrac) {
      int nbShorterLines = nbCols * nbLines - n;
      int nbFullLines = nbLines - nbShorterLines;
      double xStep = size.x() / nbCols;
      double yStep = size.y() / nbLines;
      ImmutableSet.Builder<PositiveSize> builder = ImmutableSet.builder();
      for (int col = 0; col < nbCols - 1; ++col) {
        for (int line = nbLines - 1; line >= 0; --line) {
          builder.add(new PositiveSize((col + additionalColFrac) * xStep,
              (line + additionalColFrac) * yStep));
        }
      }
      int col = nbCols - 1;
      for (int line = nbLines - 1; line > nbLines - 1 - nbFullLines; --line) {
        builder.add(new PositiveSize((col + additionalColFrac) * xStep,
            (line + additionalColFrac) * yStep));
      }
      ImmutableSet<PositiveSize> offsets = builder.build();
      verify(offsets.size() == n);
      return offsets;
    }
  }

  private static record RepresentableSubZone (PositiveSize absoluteOffset, Representation repr,
      RepresentableZone parent) {
    public PositiveSize size() {
      return parent.subSize();
    }

    /** A positive finite double if non-empty string; otherwise positive infinity. */
    public double maxWidthPerCp() {
      if (!repr.isString()) {
        return Double.POSITIVE_INFINITY;
      }
      return size().x() / repr.string().codePoints().count();
    }

    public PositiveSize absoluteOffsetToMiddle() {
      return absoluteOffset.plus(size().mult(0.5d));
    }
  }

  private static record RepresentableZone (RectangleElement zone,
      ImmutableList<Representation> reprs) {
    public DoublePoint zoneStart() {
      return zone.getStart();
    }

    public PositiveSize zoneSize() {
      return zone.getSize();
    }

    public PositiveSize startOffset() {
      return PositiveSize.between(DoublePoint.zero(), zoneStart());
    }

    public LineColDivision div() {
      return LineColDivision.forNb(reprs.size());
    }

    public PositiveSize subSize() {
      return PositiveSize.given(zoneSize().x() / div().nbCols, zoneSize().y() / div().nbLines);
    }

    public ImmutableSet<PositiveSize> relativeOffsets() {
      return div().offsetsToCorners(zoneSize());
    }

    public ImmutableSet<PositiveSize> absoluteOffsets() {
      return relativeOffsets().stream().map(offset -> startOffset().plus(offset))
          .collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableSet<RepresentableSubZone> subZones() {
      ImmutableSet<PositiveSize> offsets = absoluteOffsets();
      UnmodifiableIterator<Representation> rIt = reprs.iterator();
      final ImmutableSet.Builder<RepresentableSubZone> subs = new ImmutableSet.Builder<>();
      for (PositiveSize offset : offsets) {
        Representation r = rIt.next();
        subs.add(new RepresentableSubZone(offset, r, this));
      }
      verify(!rIt.hasNext());
      return subs.build();
    }

    public double maxWidthPerCp() {
      return subZones().stream().mapToDouble(RepresentableSubZone::maxWidthPerCp).min()
          .orElse(Double.POSITIVE_INFINITY);
    }
  }

  /** TODO move to SVGHelper */
  public static Optional<PositiveSize> size(Element svgElement) {
    if (svgElement.hasAttribute("width") && svgElement.hasAttribute("height")) {
      PositiveSize size = PositiveSize.given(Double.parseDouble(svgElement.getAttribute("width")),
          Double.parseDouble(svgElement.getAttribute("height")));
      return Optional.of(size);
    }
    return Optional.empty();
  }

  private static Element toSvg(SvgDocumentHelper h, RepresentableSubZone subZone) {
    final Representation r = subZone.repr;
    if (r.isString()) {
      PositiveSize halfSize = subZone.size().mult(0.5d);
      return h.text().setBaselineStart(DoublePoint.given(halfSize.x(), halfSize.y()))
          .setContent(r.string()).getElement();
    }
    Element svgRepr = (Element) h.document().importNode(r.svg().getDocumentElement(), true);
    if (size(svgRepr).isEmpty()) {
      SvgHelper.setSize(svgRepr, subZone.size());
    }
    return svgRepr;
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
    SvgKeyboard kb = new SvgKeyboard(h);

    String inner = """
        fill-opacity: 0;
        stroke: black;
        stroke-width: 1px;""";
    kb.appendStyle(RectangleElement.NODE_NAME, inner);
    return kb;
  }

  public static SvgKeyboard using(Document doc) {
    return new SvgKeyboard(SvgDocumentHelper.using(doc));
  }

  private final SvgDocumentHelper h;
  private double fontSize = Double.NaN;

  private SvgKeyboard(SvgDocumentHelper h) {
    this.h = h;
  }

  public Document document() {
    return h.document();
  }

  private void appendStyle(String element, String inner) {
    String content = element + " {\n" + inner + "\n}";
    StyleElement style = h.style().setContent(content);
    h.document().getDocumentElement().insertBefore(style.getElement(),
        h.document().getDocumentElement().getFirstChild());
  }

  public ImmutableMap<RectangleElement, String> keyNameByZone() {
    ImmutableMap.Builder<RectangleElement, String> reprsBuilder = ImmutableMap.builder();
    for (Element rect : getElements(h.document().getDocumentElement(), SVG_RECT_NAME)) {
      if (!hasAttribute(rect, KEYBOARDD_X_KEY_NAME)) {
        continue;
      }
      String xKeyName = getAttribute(rect, KEYBOARDD_X_KEY_NAME);
      reprsBuilder.put(RectangleElement.using(rect), xKeyName);
    }
    return reprsBuilder.build();
  }

  public double maxWidthPerCp(Function<String, ? extends List<String>> descriptionsByXKeyName) {
    Function<String, ImmutableList<Representation>> representationsByXKeyName =
        s -> descriptionsByXKeyName.apply(s).stream().map(Representation::fromString)
            .collect(ImmutableList.toImmutableList());
    ImmutableSet<RepresentableZone> zones = getZones(representationsByXKeyName);
    return maxWidthPerCp(zones);
  }

  private double maxWidthPerCp(Set<RepresentableZone> zones) {
    return zones.stream().mapToDouble(t -> t.maxWidthPerCp()).min()
        .orElse(Double.POSITIVE_INFINITY);
  }

  /** NaN for maxWidthPerCp (default) */
  public SvgKeyboard setFontSize(double fontSize) {
    this.fontSize = fontSize;
    return this;
  }

  private double fontSize(Set<RepresentableZone> zones) {
    if (!Double.isNaN(fontSize)) {
      return fontSize;
    }
    return maxWidthPerCp(zones);
  }

  /**
   * Adds representations to the zones found in this document, according to the given function.
   *
   * @param representationsByXKeyName the respective representations to add to the zones.
   * @return the document with the added representations.
   */
  public Document withRepresentations(
      Function<String, ? extends List<Representation>> representationsByXKeyName) {
    // Thanks to https://stackoverflow.com/questions/5226852/cloning-dom-document-object . TODO
    // document in Jaris?
    // DOMResult result = new DOMResult();
    // XmlTransformer.usingFoundFactory().usingEmptyStylesheet().transform(new DOMSource(doc),
    // result);
    // Document d = (Document) result.getNode();

    ImmutableSet<RepresentableZone> zones = getZones(representationsByXKeyName);
    // it’s very unlikely that the font size will be constrained in height, so let’s just consider
    // the available width. We consider that 1px font size (which determines the height of am em
    // box) is about a 1px car wide. A very rough approximation, to be sure.
    double effectiveFontSize = fontSize(zones);
    if (Double.isFinite(effectiveFontSize)) {
      String inner = """
          text-anchor: middle;
          dominant-baseline: middle;
          font-size: %spx;""".formatted(effectiveFontSize);
      appendStyle(TextElement.NODE_NAME, inner);
    }

    for (RepresentableZone zone : zones) {
      for (RepresentableSubZone r : zone.subZones()) {
        Element g = h.g().translate(r.absoluteOffset).getElement();
        Element svgRepr = toSvg(h, r);
        g.appendChild(svgRepr);
        Node prev = zone.zone.getElement().getNextSibling();
        zone.zone.getElement().getParentNode().insertBefore(g, prev);
      }
    }
    return h.document();
  }

  private ImmutableSet<RepresentableZone>
      getZones(Function<String, ? extends List<Representation>> representationsByXKeyName) {
    ImmutableMap<RectangleElement, String> keyNameByZone = keyNameByZone();
    ImmutableSet<RepresentableZone> zones = keyNameByZone.keySet().stream().map(zone -> {
      String xKeyName = keyNameByZone.get(zone);
      List<Representation> reprs = representationsByXKeyName.apply(xKeyName);
      return new RepresentableZone(zone, ImmutableList.copyOf(reprs));
    }).collect(ImmutableSet.toImmutableSet());
    return zones;
  }
}
