package io.github.oliviercailloux.keyboardd;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;
import io.github.oliviercailloux.svgb.SvgDocumentHelper;

public class Drawer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Drawer.class);

  public static void main(String[] args) throws Exception {
    new Drawer().proceed();
  }

  public void readJson() {
    // Jsonb jsonb = JsonbBuilder.create();
    // Book book = jsonb.fromJson(new FileReader("jsonfile.json"), Book.class);
  }

  public void proceed() throws IOException {
    /* Use wev (speaking in base 10) to read keycodes for physical keys such as 10 for key 1 (also gives keysyms depending on logical state such as 38, &, or 49, 1). The file /usr/share/X11/xkb/keycodes/evdev contains the mappings keycodes (base 10) to key names such as AE01 for keycode 10. 
     * 
     * Also, key F1 sends keycode 67, F2 sends keycode 68, Fn+F1 sends keycode 179, sym XF86Tools (269025153), Fn+F2 sends keycode 122, sym XF86AudioLowerVolume (269025041). evdev maps keycode 179 to I179 and keycode 122 to VOL-.
     * 
     * To capture codes for the left Windows and the IMPÉCR keys, I had to use sudo libinput debug-events, which gives codes 8 less than wev (Linux kernel codes?), but which gives no code for many keys such as A or F1 or &.
    */
    CharSource source =
        Resources.asCharSource(this.getClass().getResource("fr"), StandardCharsets.UTF_8);
    XkbReader.read(null);
    LOGGER.info("Drawing key names.");
    final DomHelper d = DomHelper.domHelper();
    final Document doc = d.svg();
    final SvgDocumentHelper h = SvgDocumentHelper.using(doc);
    //"210"+" "+"297"
    h.setSize(PositiveSize.given(210d, 297d));
    h.square().setStart(new DoublePoint(10d, 10d)).setSize(10d);
  }
}
