package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * A more general concept than a RectangularKeyboard (because keys do not have to be rectangle here).
 * Useful in FunctionalKeyboard: parse SVG so as to have the mapping of geometric keys and x key
 * names.
 * <p>TODO
 * Parse doc.
 * Determine els to erase
 * Determine els with keys
 * Local variables:
 * - doc read (input), a copy
 * lazyBuildErasables(): nope, no use of that!
 * lazyBuildKeyElements()
 * - can query the doc (returns a copy of the copy)
 public Document toDocument() {
   return a copy (for defense);
 }
 * - can NOT query the erasables and stuff of the local copy: as we need to build defensive copies, this would not be useful.
 * - can query the erasables and stuff of an own provided copy.
 
- 
  public SvgKeyboard withRepresentation(VisibleKeyboardMap visibleKeyboardMap) {

 * doc being written to: helper (not in instance, just local variable while building new instance or in SvgKeyboardBuilder)
The helper gets a private copy. There, we delete stuff. Thus need to be able to query that copy!
 */
public class SvgKeyboard {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(SvgKeyboard.class);

  private static final String SVG_NS = SvgDocumentHelper.SVG_NS_URI.toString();
  private static final URI KEYBOARDD_NS = URI.create("https://io.github.oliviercailloux.keyboardd");
  private static final String KEYBOARDD_X_KEY_NAME_LOCAL_NAME = "x-key-name";
  private static final XmlName KEYBOARDD_X_KEY_NAME = XmlName.expandedName(KEYBOARDD_NS, KEYBOARDD_X_KEY_NAME_LOCAL_NAME);

  /**
   * @deprecated Use JARiS.
   */
  @Deprecated()
  private static boolean hasAttribute(Element element, XmlName name) {
    return element.hasAttributeNS(name.namespace().map(URI::toString).orElse(null), name.localName());
  }

  /**
   * @deprecated Use JARiS.
   */
  @Deprecated()
  private static String getAttribute(Element element, XmlName name) {
    checkArgument(hasAttribute(element, name));
    return element.getAttributeNS(name.namespace().map(URI::toString).orElse(null), name.localName());
  }

  public static ImmutableMap<Element, String> reprs(Element root) {
    ImmutableMap.Builder<Element, String> reprsBuilder = ImmutableMap.builder();
    for (Element rect : DomHelper.toElements(root.getElementsByTagNameNS(SVG_NS, "rect"))) {
      if (!hasAttribute(rect, KEYBOARDD_X_KEY_NAME))
        continue;
      String xKeyName = getAttribute(rect, KEYBOARDD_X_KEY_NAME);
      reprsBuilder.put(rect, xKeyName);
    }
    return reprsBuilder.build();
  }

  public static SvgKeyboard parse(Document doc) {
    return new SvgKeyboard(doc);
  }

  private final Document doc;

  private SvgKeyboard(Document doc) {
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

    this.doc = doc;
  }
}
