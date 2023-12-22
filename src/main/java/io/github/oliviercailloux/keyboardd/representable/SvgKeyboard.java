package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKeyboard;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalRowKey;
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

  /** 1 unit in the given physical keyboard is rendered as 1 cm at 96 DPI (thus as 96/2.54 ≅ 38 dots) */
  public static SvgKeyboard from(PhysicalKeyboard physicalKeyboard) {
    final DomHelper d = DomHelper.domHelper();
    final Document doc = d.svg();
    final SvgDocumentHelper h = SvgDocumentHelper.using(doc);
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
        RectangleElement rect = h.rectangle().setRounding(10d).setStart(posScaled).setSize(sizeScaled);
        rect.getElement().setAttribute("x-key-name", key.xKeyName());
        doc.getDocumentElement().appendChild(rect.getElement());
        reprsBuilder.put(rect.getElement(), key.xKeyName());
        coveredsBuilder.add(rect.getElement());
        TextElement text =
            h.text().setBaselineStart(posScaled.plus(sizeScaled.mult(0.5d))).setContent(key.xKeyName());
            doc.getDocumentElement().appendChild(text.getElement());
            coveredsBuilder.add(text.getElement());
      }

    return new SvgKeyboard(doc, reprsBuilder.build(), coveredsBuilder.build());
  }

  public static SvgKeyboard parse(Document doc) {
    ImmutableMap.Builder<Element, String> reprsBuilder = ImmutableMap.builder();
    ImmutableSet.Builder<Element> coveredsBuilder = ImmutableSet.builder();
    for (Element rect : DomHelper.toElements(doc.getElementsByTagName("rect"))) {
      if(!rect.hasAttribute("x-key-name")) continue;
      String xKeyName = rect.getAttribute("x-key-name");
      reprsBuilder.put(rect, xKeyName);
      coveredsBuilder.add(rect);
    }
    for (Element text : DomHelper.toElements(doc.getElementsByTagName("text"))) {
      if((!text.hasAttribute("x")) || (!text.hasAttribute("y"))) continue;
      /** TODO check inclusion. */
      coveredsBuilder.add(text);
    }

    return new SvgKeyboard(doc, reprsBuilder.build(), coveredsBuilder.build());
  }

  private final Document doc;
  private final ImmutableMap<Element, String> canonicalNameByKeyRepresentationZone;
  /*
   * The elements that are entirely covered by representation zones (thus, including the
   * representation zones).
   */
  private final ImmutableSet<Element> coveredElements;

  private SvgKeyboard(Document doc, Map<Element, String> canonicalNameByKeyRepresentationZone,
      Set<Element> coveredElements) {
    this.doc = doc;
    this.canonicalNameByKeyRepresentationZone = ImmutableMap.copyOf(canonicalNameByKeyRepresentationZone);
    this.coveredElements = ImmutableSet.copyOf(coveredElements);
  }
}
