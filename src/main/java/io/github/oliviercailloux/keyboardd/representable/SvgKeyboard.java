package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.math.IntMath;
import io.github.oliviercailloux.geometry.Displacement;
import io.github.oliviercailloux.geometry.Point;
import io.github.oliviercailloux.geometry.Zone;
import io.github.oliviercailloux.jaris.collections.CollectionUtils;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import io.github.oliviercailloux.svgb.SvgHelper;
import io.github.oliviercailloux.svgb.TextElement;
import java.math.RoundingMode;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

  private static DecimalFormat DECIMAL_FORMAT =
      new DecimalFormat("0.####", new DecimalFormatSymbols(Locale.US));

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
   * @deprecated Move to JARiS.
   */
  @Deprecated()
  private static void setAttribute(Element element, XmlName name, String value) {
    element.setAttributeNS(name.namespace().map(URI::toString).orElse(null), name.localName(),
        value);
  }

  private static record LineColDivision (int n, int nbCols) {
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
      return new LineColDivision(n, x);
    }

    LineColDivision {
      verify((n == 0) == (nbCols == 0));
    }

    int nbLines() {
      return nbCols == 0 ? 0 : IntMath.divide(n, nbCols, RoundingMode.CEILING);
    }

    int nbFullLines() {
      return nbCols == 0 ? 0 : IntMath.divide(n, nbCols, RoundingMode.FLOOR);
    }

    int nbColsOnShorterLine() {
      return n - nbCols * nbFullLines();
    }

    int nbCols(int lineNb) {
      return lineNb == 0 && hasShorterLine() ? nbColsOnShorterLine() : nbCols;
    }

    boolean hasShorterLine() {
      return nbColsOnShorterLine() > 0;
    }

    private ImmutableSortedSet<Zone> subZones(Zone entireZone) {
      Displacement subDisplacement = subDisplacement(entireZone);
      Point currentStartOfLine = entireZone.start();
      ImmutableSortedSet.Builder<Zone> builder = ImmutableSortedSet.orderedBy(Comparator
          .comparing(Zone::start, Comparator.comparing(Point::x).thenComparing(Comparator.comparing(Point::y).reversed())));
      for (int line = 0; line < nbLines(); ++line) {
        Point currentStart = currentStartOfLine;
        for (int col = 0; col < nbCols(line); ++col) {
          builder.add(Zone.cornerMove(currentStart, subDisplacement));
          currentStart = currentStart.plus(subDisplacement.horizontal());
        }
        currentStartOfLine = currentStartOfLine.plus(subDisplacement.vertical());
      }
      ImmutableSortedSet<Zone> subs = builder.build();
      verify(subs.size() == n);
      return subs;
    }

    public Displacement subDisplacement(Zone entireZone) {
      return entireZone.across().mult(1d / nbCols, 1d / nbLines());
    }
  }

  private static record RepresentableSubZone (Zone subZone, Representation repr) {

    /** A positive finite double if non-empty string; otherwise positive infinity. */
    public double maxWidthPerCp() {
      if (!repr.isString()) {
        return Double.POSITIVE_INFINITY;
      }
      return subZone.size().mult(1d / repr.string().codePoints().count()).x();
    }

    public Element toSvg(SvgDocumentHelper h) {
      if (repr.isString()) {
        return toStringSvg(h);
      }
      return toReprSvg(h);
    }

    private Element toStringSvg(SvgDocumentHelper h) {
      return h.text().setBaselineStart(subZone.center()).setContent(repr.string()).element();
    }

    private Element toReprSvg(SvgDocumentHelper h) {
      Element svg = repr.svg().getDocumentElement();
      Optional<Point> svgSizeOpt = SvgHelper.tryGetSize(svg);
      Element importedSvg = (Element) h.document().importNode(svg, true);
      Point svgSizeMaxSubZone;
      if (svgSizeOpt.isEmpty()) {
        svgSizeMaxSubZone = subZone.size();
      } else {
        Point svgSize = svgSizeOpt.orElseThrow(VerifyException::new);
        if (svgSize.x() > subZone.size().x() || svgSize.y() > subZone.size().y()) {
          svgSizeMaxSubZone = subZone.size();
        } else {
          svgSizeMaxSubZone = svgSize;
        }
      }
      // Point gap = subZone.size().plus(svgSizeMaxSubZone.opposite());
      // Point halfGap = gap.mult(0.5d);
      // Point elemPos = subZone.start().plus(halfGap);
      Zone posAndSize = Zone.centered(subZone.center(), svgSizeMaxSubZone);
      SvgHelper.setPosition(importedSvg, posAndSize.start());
      SvgHelper.setSize(importedSvg, svgSizeMaxSubZone);
      return importedSvg;
    }
  }

  private static record RepresentableZone (RectangleElement rectangle,
      ImmutableList<Representation> reprs) {
    private LineColDivision div() {
      return LineColDivision.forNb(reprs.size());
    }

    public ImmutableSet<RepresentableSubZone> subRepresentables(Displacement shift) {
      ImmutableSortedSet<Zone> subZones = div().subZones(
          Zone.cornerMove(rectangle.zone().start().plus(shift), rectangle.zone().across()));
      UnmodifiableIterator<Representation> rIt = reprs.iterator();
      final ImmutableSet.Builder<RepresentableSubZone> subRepresentables =
          new ImmutableSet.Builder<>();
      for (Zone subZone : subZones) {
        Representation r = rIt.next();
        subRepresentables.add(new RepresentableSubZone(subZone, r));
      }
      verify(!rIt.hasNext());
      return subRepresentables.build();
    }

    public double maxWidthPerCp() {
      return subRepresentables(Displacement.noMove()).stream()
          .mapToDouble(RepresentableSubZone::maxWidthPerCp).min().orElse(Double.POSITIVE_INFINITY);
    }
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

    Point start = Point.zero();
    for (RectangularKey key : physicalKeyboard.keys()) {
      Point posScaled = start.plus(key.topLeftCorner()).mult(dotsPerCm);
      Point sizeScaled = key.size().mult(dotsPerCm);
      RectangleElement rect =
          h.rectangle(Zone.cornerMove(posScaled, Displacement.between(Point.zero(), sizeScaled)))
              .setRounding(10d);
      String xKeyName = key.xKeyName();
      if (!xKeyName.isEmpty()) {
        setAttribute(rect.element(), KEYBOARDD_X_KEY_NAME, xKeyName);
      }
      doc.getDocumentElement().appendChild(rect.element());
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
    h.document().getDocumentElement().insertBefore(style.element(),
        h.document().getDocumentElement().getFirstChild());
  }

  public ImmutableSetMultimap<String, RectangleElement> keyBindingZonesByXKeyName() {
    ImmutableMap.Builder<RectangleElement, String> reprsBuilder = ImmutableMap.builder();
    for (Element rect : getElements(h.document().getDocumentElement(), SVG_RECT_NAME)) {
      if (!DomHelper.hasAttribute(rect, KEYBOARDD_X_KEY_NAME)) {
        continue;
      }
      String xKeyName = DomHelper.getAttribute(rect, KEYBOARDD_X_KEY_NAME);
      reprsBuilder.put(RectangleElement.using(rect), xKeyName);
    }
    return reprsBuilder.build().asMultimap().inverse();
  }

  public double maxWidthPerCp(Function<String, ? extends List<String>> descriptionsByXKeyName) {
    XKeyNamesRepresenter representationsByXKeyName = s -> descriptionsByXKeyName.apply(s).stream()
        .map(Representation::fromString).collect(ImmutableList.toImmutableList());
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
  public Document withRepresentations(XKeyNamesRepresenter representationsByXKeyName) {
    ImmutableSet<RepresentableZone> zones = getZones(representationsByXKeyName);
    // it’s very unlikely that the font size will be constrained in height, so let’s just consider
    // the available width. We consider that 1px font size (which determines the height of am em
    // box) is about a 1px car wide. A very rough approximation, to be sure.
    double effectiveFontSize = fontSize(zones);
    if (Double.isFinite(effectiveFontSize)) {
      String inner = """
          text-anchor: middle;
          dominant-baseline: middle;
          font-size: %spx;""".formatted(DECIMAL_FORMAT.format(effectiveFontSize));
      appendStyle(TextElement.NODE_NAME, inner);
    }

    for (RepresentableZone zone : zones) {
      Displacement shift = Displacement.between(Point.origin(), zone.rectangle().zone().start());
      Element g = h.g().translate(shift).getElement();
      Node next = zone.rectangle.element().getNextSibling();
      zone.rectangle.element().getParentNode().insertBefore(g, next);
      for (RepresentableSubZone r : zone.subRepresentables(shift.opposite())) {
        Element svgRepr = r.toSvg(h);
        g.appendChild(svgRepr);
      }
    }
    return h.document();
  }

  private ImmutableSet<RepresentableZone> getZones(XKeyNamesRepresenter representationsByXKeyName) {
    ImmutableSet<RepresentableZone> zones =
        keyBindingZonesByXKeyName().values().stream().map(rect -> {
          String xKeyName = DomHelper.getAttribute(rect.element(), KEYBOARDD_X_KEY_NAME);
          List<Representation> reprs = representationsByXKeyName.representations(xKeyName);
          return new RepresentableZone(rect, ImmutableList.copyOf(reprs));
        }).collect(ImmutableSet.toImmutableSet());
    return zones;
  }
}
