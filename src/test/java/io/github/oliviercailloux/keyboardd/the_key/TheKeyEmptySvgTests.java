package io.github.oliviercailloux.keyboardd.the_key;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularKeyboardReader;
import io.github.oliviercailloux.keyboardd.representable.RectangularKeyboard;
import io.github.oliviercailloux.keyboardd.representable.Representation;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import io.github.oliviercailloux.svgb.PositiveSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class TheKeyEmptySvgTests {
  private static final double KEYS_WIDTH_CM = 1d;
  private static final double KEYS_HEIGHT_CM = 1d;
  private static final double KEYS_HORIZONTAL_SPACING_CM = 0.2d;

  @Test
  public void writeSvg() throws IOException {
    CharSource json = Resources.asCharSource(
        getClass().getResource("The Key.json"), StandardCharsets.UTF_8);
    CharSource svg = Resources.asCharSource(
        getClass().getResource("The Key.svg"), StandardCharsets.UTF_8);

    RectangularKeyboard keyboard = JsonRectangularKeyboardReader.rowKeyboard(json)
        .toPhysicalKeyboard(PositiveSize.given(KEYS_WIDTH_CM, KEYS_HEIGHT_CM), PositiveSize.given(KEYS_HORIZONTAL_SPACING_CM, 0d));
    Document svgDoc = SvgKeyboard.zonedFrom(keyboard).document();
    String svgString = DomHelper.domHelper().toString(svgDoc);
    assertEquals(svg.read(), svgString);
  }
}
