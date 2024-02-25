package io.github.oliviercailloux.keyboardd.representable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.RectangularKeyboard;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularKeyboardReader;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularRowKeyboard;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMapTests;
import io.github.oliviercailloux.keyboardd.mapping.SimpleSymbolsReader;
import io.github.oliviercailloux.keyboardd.representable.Representation;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import io.github.oliviercailloux.keyboardd.representable.VisibleKeyboardMap;
import io.github.oliviercailloux.svgb.PositiveSize;

public class DrawerTests {
  @Test
  public void testZones() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonRectangularKeyboardReader.class.getResource("Keyboard layout simple.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(DrawerTests.class.getResource("Keyboard 2 keys.svg").toURI()));

    JsonRectangularRowKeyboard layout = JsonRectangularKeyboardReader.rowKeyboard(source);
    RectangularKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    SvgKeyboard svgK = SvgKeyboard.zonedFrom(physicalKeyboard);
    String svg = DomHelper.domHelper().toString(svgK.document());
    Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }

  @Test
  public void testTrivialRepresentation() throws Exception {
    Document zoned = DomHelper.domHelper().asDocument(new StreamSource(DrawerTests.class.getResource("Keyboard 2 keys.svg").toString()));
    String expected = Files
        .readString(Path.of(SvgKeyboard.class.getResource("Keyboard 2 keys trivial representation.svg").toURI()));

    SvgKeyboard svgK = SvgKeyboard.using(zoned).withRepresentations(k -> ImmutableList.of(Representation.fromString(k)));
    String svg = DomHelper.domHelper().toString(svgK.document());
    Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }

  @Test
  public void testReDraw() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonRectangularKeyboardReader.class.getResource("Keyboard layout simple.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(SvgKeyboard.class.getResource("Keyboard simple scale 2.svg").toURI()));

    JsonRectangularRowKeyboard layout = JsonRectangularKeyboardReader.rowKeyboard(source);
    RectangularKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    SvgKeyboard svgK = SvgKeyboard.from(physicalKeyboard);
    CharSource kbMapSource =
        Resources.asCharSource(KeyboardMapTests.class.getResource("Two keys"), StandardCharsets.UTF_8);
    KeyboardMap kbMap = SimpleSymbolsReader.read(kbMapSource);
    SvgKeyboard svgR = svgK.withRepresentation(VisibleKeyboardMap.from(kbMap, ImmutableMap.of()));
    String svg = DomHelper.domHelper().toString(svgR.getDocument());
    Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }
}
