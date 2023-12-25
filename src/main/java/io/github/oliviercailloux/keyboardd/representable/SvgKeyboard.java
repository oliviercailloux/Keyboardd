package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Verify.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.UnmodifiableIterator;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKeyboard;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import io.github.oliviercailloux.svgb.TextElement;

/*
 * A more general concept than a PhysicalKeyboard (because keys do not have to be rectangle here).
 * Useful in FunctionalKeyboard: parse SVG so as to have the mapping of geometric keys and x key
 * names.
 */
public class SvgKeyboard {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(SvgKeyboard.class);

  public static void main(String[] args) {
    final DomHelper d = DomHelper.domHelper();
    final SvgDocumentHelper hDoc = SvgDocumentHelper.using(d);
    final Document doc = hDoc.document();
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:kdd",
    KEYBOARDD_NS);
    /*
     * Not sure the dominant baseline trick is appropriate when the baselines are not uniform (p VS
     * t), but that’ll do for now as we write capital letters.
     */
    StyleElement style = hDoc.style().setContent("""
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

    DoublePoint start = DoublePoint.zero();
    ImmutableMap.Builder<Element, String> reprsBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<Element> coveredsBuilder = ImmutableSet.builder();
    PhysicalKey key = PhysicalKey.from(DoublePoint.zero(), PositiveSize.square(1d), "Name");
      DoublePoint posScaled = start.plus(key.topLeftCorner()).mult(dotsPerCm);
      PositiveSize sizeScaled = key.size().mult(dotsPerCm);
      RectangleElement rect =
          hDoc.rectangle().setRounding(10d).setStart(posScaled).setSize(sizeScaled);
      rect.getElement().setAttributeNS(KEYBOARDD_NS, "kdd:x-key-name", key.xKeyName());
      doc.getDocumentElement().appendChild(rect.getElement());
      reprsBuilder.put(rect.getElement(), key.xKeyName());
      TextElement text = hDoc.text().setBaselineStart(posScaled.plus(sizeScaled.mult(0.5d)))
          .setContent(key.xKeyName());
      doc.getDocumentElement().appendChild(text.getElement());
      Element covered = text.getElement();
      coveredsBuilder.add(covered);

    ImmutableSet<Element> coveredElements = coveredsBuilder.build();
    ImmutableMap<Element, String> canonicalNameByKeyRepresentationZone = reprsBuilder.build();
    SvgKeyboard svgKeyboard = new SvgKeyboard(doc, canonicalNameByKeyRepresentationZone, coveredElements);

    DOMResult result = new DOMResult();
    XmlTransformer.usingFoundFactory().usingEmptyStylesheet().transform(new DOMSource(doc), result);
    Document docCopy = (Document) result.getNode();
    // docCopy.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:kddd",
    //     KEYBOARDD_NS);
    ImmutableSet<Element> ourElements = coveredElements;
    ImmutableSet.Builder<Element> correspondingElementsBuilder = ImmutableSet.builder();
    ImmutableSet<Element> correspondingElements = correspondingElementsBuilder.build();
    
      Element root = doc.getDocumentElement();
      verify(!covered.equals(root));
      ImmutableList.Builder<Integer> builder = ImmutableList.builder();
      Element current = covered;
      do {
        Element parent = (Element) current.getParentNode();
        ImmutableList<Node> children = DomHelper.toList(parent.getChildNodes());
        int index = children.indexOf(current);
        builder.add(index);
        current = parent;
      } while (!current.equals(root));
      ImmutableList<Integer> located = builder.build().reverse();
    
    // for (Element covered : ourElements) {
      correspondingElementsBuilder.add(retrieveFrom(docCopy, located));
    // }
    for (Element element : correspondingElements) {
      element.getParentNode().removeChild(element);
    }
    ImmutableMap<Element, String> nameByZone = DomHelper
        .toElements(docCopy.getElementsByTagNameNS(SVG_NS, "rect")).stream()
        .filter(e -> e.hasAttributeNS(KEYBOARDD_NS, "x-key-name")).collect(
            ImmutableMap.toImmutableMap(e -> e, e -> e.getAttributeNS(KEYBOARDD_NS, "x-key-name")));
    ImmutableList<RectangleElement> allZones = nameByZone.keySet().stream()
        .map(RectangleElement::using).collect(ImmutableList.toImmutableList());
    ImmutableSet.Builder<Element> covereds = ImmutableSet.builder();
    SvgDocumentHelper h = SvgDocumentHelper.using(docCopy);
    String xKeyName = "Name";
      ImmutableList<RectangleElement> zones = allZones.stream()
          .filter(e -> e.getElement().getAttributeNS(KEYBOARDD_NS, "x-key-name").equals(xKeyName))
          .collect(ImmutableList.toImmutableList());
      for (RectangleElement zone : zones) {
        zone.getElement().setAttributeNS(KEYBOARDD_NS, "kdd:x-key-name", xKeyName);
        PositiveSize startOffset = PositiveSize.between(DoublePoint.zero(), start);
        LineColDivision div = LineColDivision.forNb(1);
        ImmutableSet<PositiveSize> offsets = div.offsets(PositiveSize.square(1d));
        UnmodifiableIterator<PositiveSize> offsetsIt = offsets.iterator();
        Representation r = Representation.fromString("NameRepr");
          PositiveSize offset = offsetsIt.next();
          Element svgRepr = toSvg(h, r);
          Element g = h.g().translate(startOffset.plus(offset)).getElement();
          g.appendChild(svgRepr);
          Node prev = zone.getElement().getNextSibling();
          zone.getElement().getParentNode().insertBefore(g, prev);
          covereds.add(g);
          covereds.add(svgRepr);
        verify(!offsetsIt.hasNext());
    }
    SvgKeyboard resultSvg = new SvgKeyboard(docCopy, nameByZone, covereds.build());
    LOGGER.info(d.toString(resultSvg.getDocument()));
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

  private static final String SVG_NS = SvgDocumentHelper.SVG_NS_URI.toString();
  private static final String KEYBOARDD_NS = "https://io.github.oliviercailloux.keyboardd";

  /**
   * 1 unit in the given physical keyboard is rendered as 1 cm at 96 DPI (thus as 96/2.54 ≅ 38 dots)
   */
  public static SvgKeyboard from(PhysicalKeyboard physicalKeyboard) {
    final DomHelper d = DomHelper.domHelper();
    final SvgDocumentHelper h = SvgDocumentHelper.using(d);
    final Document doc = h.document();
    doc.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:kdd",
        KEYBOARDD_NS);
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

    DoublePoint start = DoublePoint.zero();
    ImmutableMap.Builder<Element, String> reprsBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<Element> coveredsBuilder = ImmutableSet.builder();
    for (PhysicalKey key : physicalKeyboard.keys()) {
      DoublePoint posScaled = start.plus(key.topLeftCorner()).mult(dotsPerCm);
      PositiveSize sizeScaled = key.size().mult(dotsPerCm);
      RectangleElement rect =
          h.rectangle().setRounding(10d).setStart(posScaled).setSize(sizeScaled);
      rect.getElement().setAttributeNS(KEYBOARDD_NS, "kdd:x-key-name", key.xKeyName());
      doc.getDocumentElement().appendChild(rect.getElement());
      /*
       * TODO should not include the text elements here: should produce an SVG with only the
       * representation zones, then use the standard algorithm to add the texts.
       */
      reprsBuilder.put(rect.getElement(), key.xKeyName());
      TextElement text = h.text().setBaselineStart(posScaled.plus(sizeScaled.mult(0.5d)))
          .setContent(key.xKeyName());
      doc.getDocumentElement().appendChild(text.getElement());
      coveredsBuilder.add(text.getElement());
    }

    return new SvgKeyboard(doc, reprsBuilder.build(), coveredsBuilder.build());
  }

  public static SvgKeyboard parse(Document doc) {
    ImmutableMap.Builder<Element, String> reprsBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<Element> coveredsBuilder = ImmutableSet.builder();
    for (Element rect : DomHelper.toElements(doc.getElementsByTagNameNS(SVG_NS, "rect"))) {
      if (!rect.hasAttributeNS(KEYBOARDD_NS, "x-key-name"))
        continue;
      String xKeyName = rect.getAttributeNS(KEYBOARDD_NS, "x-key-name");
      reprsBuilder.put(rect, xKeyName);
    }
    for (Element text : DomHelper.toElements(doc.getElementsByTagNameNS(SVG_NS, "text"))) {
      // if((!text.hasAttribute("x")) || (!text.hasAttribute("y"))) continue;
      /** TODO check inclusion. */
      coveredsBuilder.add(text);
    }

    return new SvgKeyboard(doc, reprsBuilder.build(), coveredsBuilder.build());
  }

  private static Element retrieveFrom(Document doc, List<Integer> location) {
    Element current = doc.getDocumentElement();
    for (int index : location) {
      ImmutableList<Node> children = DomHelper.toList(current.getChildNodes());
      current = (Element) children.get(index);
    }
    return current;
  }

  private final Document doc;
  private final ImmutableMap<Element, String> canonicalNameByKeyRepresentationZone;
  /*
   * The elements that are entirely covered by representation zones, excluding the representation
   * zones.
   */
  private final ImmutableSet<Element> coveredElements;

  private SvgKeyboard(Document doc, Map<Element, String> canonicalNameByKeyRepresentationZone,
      Set<Element> coveredElements) {
    this.doc = doc;
    this.canonicalNameByKeyRepresentationZone =
        ImmutableMap.copyOf(canonicalNameByKeyRepresentationZone);
    this.coveredElements = ImmutableSet.copyOf(coveredElements);
  }

  private ImmutableList<Integer> locate(Element target) {
    Element root = doc.getDocumentElement();
    verify(!target.equals(root));
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    Element current = target;
    do {
      Element parent = (Element) current.getParentNode();
      ImmutableList<Node> children = DomHelper.toList(parent.getChildNodes());
      int index = children.indexOf(current);
      builder.add(index);
      current = parent;
    } while (!current.equals(root));
    return builder.build().reverse();
  }

  public Document getDocument() {
    return doc;
  }

  public SvgKeyboard withRepresentation(VisibleKeyboardMap visibleKeyboardMap) {
    // Thanks to https://stackoverflow.com/questions/5226852/cloning-dom-document-object .
    // TODO the transformer approach does not reuse the existing namespace, it ignores it and creates new prefixes. This works but is ugly. Also, we should be able to create an empty document using JARIS (otherwise import is forbidden).
    // DOMResult result = new DOMResult();
    // XmlTransformer.usingFoundFactory().usingEmptyStylesheet().transform(new DOMSource(doc), result);
    // Document docCopy = (Document) result.getNode();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
          db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
          throw new RuntimeException(e);
        }
        Document docCopy = db.newDocument();
    // final DomHelper d = DomHelper.domHelper();
    // final Document docCopy = d.svg();
    Node rootCopy = docCopy.importNode(doc.getDocumentElement(), true);
    docCopy.appendChild(rootCopy);
    // docCopy.getDocumentElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:kddd",
    //     KEYBOARDD_NS);
    ImmutableSet<Element> ourElements = coveredElements;
    ImmutableSet<Element> correspondingElements = correspondingElements(docCopy, ourElements);
    for (Element element : correspondingElements) {
      element.getParentNode().removeChild(element);
    }
    ImmutableMap<Element, String> nameByZone = DomHelper
        .toElements(docCopy.getElementsByTagNameNS(SVG_NS, "rect")).stream()
        .filter(e -> e.hasAttributeNS(KEYBOARDD_NS, "x-key-name")).collect(
            ImmutableMap.toImmutableMap(e -> e, e -> e.getAttributeNS(KEYBOARDD_NS, "x-key-name")));
    ImmutableList<RectangleElement> allZones = nameByZone.keySet().stream()
        .map(RectangleElement::using).collect(ImmutableList.toImmutableList());
    ImmutableSet.Builder<Element> covereds = ImmutableSet.builder();
    SvgDocumentHelper h = SvgDocumentHelper.using(docCopy);
    for (String xKeyName : visibleKeyboardMap.canonicalNames()) {
      ImmutableList<RectangleElement> zones = allZones.stream()
          .filter(e -> e.getElement().getAttributeNS(KEYBOARDD_NS, "x-key-name").equals(xKeyName))
          .collect(ImmutableList.toImmutableList());
      ImmutableList<Representation> reprs = visibleKeyboardMap.representations(xKeyName);
      for (RectangleElement zone : zones) {
        zone.getElement().setAttributeNS(KEYBOARDD_NS, "kdd:x-key-name", xKeyName);
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
          covereds.add(g);
          covereds.add(svgRepr);
        }
        verify(!offsetsIt.hasNext());
      }
    }
    return new SvgKeyboard(docCopy, nameByZone, covereds.build());
  }

  private static Element toSvg(SvgDocumentHelper h, Representation r) {
    if (r.isString()) {
      return h.text().setContent(r.string()).getElement();
    }
    return r.svg();
  }

  private ImmutableSet<Element> correspondingElements(Document docCopy,
      ImmutableSet<Element> ourElements) {
    ImmutableSet.Builder<Element> correspondingElementsBuilder = ImmutableSet.builder();
    for (Element covered : ourElements) {
      correspondingElementsBuilder.add(retrieveFrom(docCopy, locate(covered)));
    }
    ImmutableSet<Element> correspondingElements = correspondingElementsBuilder.build();
    return correspondingElements;
  }
}
