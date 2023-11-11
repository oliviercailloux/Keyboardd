package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Verify.verify;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;
import com.google.common.math.DoubleMath;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboard;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboardReader;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.StyleElement;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;
import io.github.oliviercailloux.svgb.TextElement;

public class Drawer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Drawer.class);

  public static void main(String[] args) throws Exception {
    JsonPhysicalKeyboard layout = JsonPhysicalKeyboardReader.parse().getLayout(
        MoreFiles.asCharSource(Path.of("Keyboard layout Elite K70.json"), StandardCharsets.UTF_8));
    Document doc = forPrint(layout);
    // Document doc = forScreen(layout);
    String svg = DomHelper.domHelper().toString(doc);
    Files.writeString(Path.of("out.svg"), svg);
  }

  static Document forPrint(JsonPhysicalKeyboard layout) {
    return draw(layout, DoublePoint.zero(), 96d);
  }

  static Document forScreen(JsonPhysicalKeyboard layout) {
    /*
     * Scaling for my bigger screen (27 ″, 2560×1440 pixels). Real diag is 68.2 cm = 26.85 ″.
     */
    double dpi = (2560d / 16d) / (26.85d / Math.sqrt(256d + 81d));
    verify(DoubleMath.fuzzyEquals(dpi, 109.39d, 1e-2d));
    Document doc = draw(layout, DoublePoint.given(1d, 1d), dpi);
    return doc;
  }

  public static Document draw(JsonPhysicalKeyboard layout, DoublePoint start, double dpi) {
    /*
     * Use wev (speaking in base 10) to read keycodes for physical keys such as 10 for key 1 (also
     * gives keysyms depending on logical state such as 38, &, or 49, 1). The file
     * /usr/share/X11/xkb/keycodes/evdev contains the mappings keycodes (base 10) to key names such
     * as AE01 for keycode 10.
     * 
     * Also, key F1 sends keycode 67, F2 sends keycode 68, Fn+F1 sends keycode 179, sym XF86Tools
     * (269025153), Fn+F2 sends keycode 122, sym XF86AudioLowerVolume (269025041). evdev maps
     * keycode 179 to I179 and keycode 122 to VOL-.
     * 
     * To capture codes for the left Windows and the IMPÉCR keys, I had to use sudo libinput
     * debug-events, which gives codes 8 less than wev (Linux kernel codes?), but which gives no
     * code for many keys such as A or F1 or &.
     */
    LOGGER.info("Drawing key names.");
    final DomHelper d = DomHelper.domHelper();
    final Document doc = d.svg();
    final SvgDocumentHelper h = SvgDocumentHelper.using(doc);
    // h.setSize(PositiveSize.given(5d, 5d));
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

    /*
     * We want to render at chosen font size, so no scaling. Thus, we have to choose the key size
     * accordingly. It is hopeless to display the real key size (in real cm), however (requires
     * knowing the number of dpi). But we can print it. I have some impression that FF prints at 96
     * DPI. Eog seems to print at 72 DPI (configurable). Let’s go for 96 DPI for the standard.
     * 
     * Firefox uses GTK3 on my system.
     */
    /* Requires Batik for BBox (on SVGSVGElement or SVGLocatable or such). */
    /* The default length and width of a key of declared length one. Target is 1 cm at 96 DPI. */
    double scale = dpi / 2.54d;
    // DoublePoint start = DoublePoint.zero();
    DoublePoint startScaled = start.mult(scale);
    /*
     * The file format should be changed to integrate the following information (instead of
     * defaulting length and height to 1d). The measurements in the file should be in SI CM.
     */
    double defaultHeight = 1.4d;
    double defaultWidth = 1.25d;
    /* inter h varies. Average is 29.6 cm for total length for 16 standard keys and 15 sep. */
    double interH = (29.6d - 16d * defaultWidth) / 15d;
    verify(DoubleMath.fuzzyEquals(interH, 0.64d, 1e-4d));
    double interV = 0.5d;
    /* Total height is 11 cm (measured), theoretically 6*height + 5*interV = 10.9 cm. */

    // LineElement line = h.line().setStart(start).setSize(PositiveSize.given(50d * dpi / 2.54d,
    // 0d))
    // .setStroke("black");
    // doc.getDocumentElement().appendChild(line.getElement());

    DoublePoint pos = startScaled;
    for (ImmutableList<JsonPhysicalKey> row : layout.rows()) {
      for (JsonPhysicalKey key : row) {
        double keyWidth = key.width() == 1d ? defaultWidth : key.width();
        PositiveSize size = PositiveSize.given(keyWidth, defaultHeight).mult(scale);
        RectangleElement rect = h.rectangle().setRounding(10d).setStart(pos).setSize(size);
        doc.getDocumentElement().appendChild(rect.getElement());
        TextElement text =
            h.text().setBaselineStart(pos.plus(size.mult(0.5d))).setContent(key.name());
        doc.getDocumentElement().appendChild(text.getElement());
        pos = pos.plus(PositiveSize.horizontal(size.x() + interH * scale));
      }
      pos = DoublePoint.given(startScaled.x(), pos.y() + (defaultHeight + interV) * scale);
    }

    return doc;
  }
}
