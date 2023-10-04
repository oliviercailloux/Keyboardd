package io.github.oliviercailloux.keyboardd;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import io.github.oliviercailloux.svgb.TextElement;

public class Drawer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Drawer.class);

  /** https://developer.mozilla.org/en-US/docs/Web/CSS/length */
  private static final double PIXELS_PER_CM = 96d / 2.54d;

  public static void main(String[] args) throws Exception {
    KeyboardLayout layout = KeyboardLayoutBuilder.parse()
        .getLayout(MoreFiles.asCharSource(Path.of("Keyboard layout.json"), StandardCharsets.UTF_8));
    Document doc = draw(layout);
    String svg = DomHelper.domHelper().toString(doc);
    Files.writeString(Path.of("out.svg"), svg);
  }

  public static Document draw(KeyboardLayout layout) {
    /* Use wev (speaking in base 10) to read keycodes for physical keys such as 10 for key 1 (also gives keysyms depending on logical state such as 38, &, or 49, 1). The file /usr/share/X11/xkb/keycodes/evdev contains the mappings keycodes (base 10) to key names such as AE01 for keycode 10. 
     * 
     * Also, key F1 sends keycode 67, F2 sends keycode 68, Fn+F1 sends keycode 179, sym XF86Tools (269025153), Fn+F2 sends keycode 122, sym XF86AudioLowerVolume (269025041). evdev maps keycode 179 to I179 and keycode 122 to VOL-.
     * 
     * To capture codes for the left Windows and the IMPÉCR keys, I had to use sudo libinput debug-events, which gives codes 8 less than wev (Linux kernel codes?), but which gives no code for many keys such as A or F1 or &.
    */
    LOGGER.info("Drawing key names.");
    final DomHelper d = DomHelper.domHelper();
    final Document doc = d.svg();
    final SvgDocumentHelper h = SvgDocumentHelper.using(doc);
    // h.setSize(PositiveSize.given(5d, 5d));
    /* Not sure the dominant baseline trick is appropriate when the baselines are not uniform (p VS t), but that’ll do for now as we write capital letters. */
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
    
    DoublePoint pos = DoublePoint.zero();
    /* We want to render at chosen font size, so no scaling. Thus, we have to choose the key size accordingly. It is hopeless to display the real key size (in real cm), however (requires knowing the number of dpi). But we can print it. I have some impression that FF prints at 96 DPI. Eog seems to print at 72 DPI (configurable). Let’s go for 96 DPI for the standard. */
    /* Requires Batik for BBox (on SVGSVGElement or SVGLocatable or such). */
    /* The default length and width of a key of declared length one. Target is 1 cm at 96 DPI.*/
    double scale = PIXELS_PER_CM;
    for (ImmutableList<KeyboardKey> row : layout.rows()) {
      for (KeyboardKey key : row) {
        PositiveSize size = PositiveSize.given(key.size(), 1d).mult(scale);
        RectangleElement rect = h.rectangle().setStart(pos).setSize(size);
        doc.getDocumentElement().appendChild(rect.getElement());
        TextElement text = h.text().setBaselineStart(pos.plus(size.mult(0.5d))).setContent(key.name());
        doc.getDocumentElement().appendChild(text.getElement());
        pos = pos.plus(PositiveSize.horizontal(size.x()));
      }
      pos.plus(PositiveSize.vertical(1d).mult(scale));
    }

    return doc;
  }
}
