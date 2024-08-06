package io.github.oliviercailloux.keyboardd.representable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularKeyboardReader;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularRowKeyboard;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMapTests;
import io.github.oliviercailloux.keyboardd.mapping.SimpleSymbolsReader;
import io.github.oliviercailloux.svgb.PositiveSize;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class SvgKeyboardTests {
  private DomHelper domHelper;

  @BeforeEach
  public void setUp() {
    domHelper = DomHelper.domHelper();
  }

  @Test
  public void testZones() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonRectangularKeyboardReader.class.getResource("Keyboard layout simple.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(SvgKeyboardTests.class.getResource("Keyboard two keys.svg").toURI()));

    JsonRectangularRowKeyboard layout = JsonRectangularKeyboardReader.rowKeyboard(source);
    RectangularKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    SvgKeyboard svgK = SvgKeyboard.zonedFrom(physicalKeyboard);
    String svg = domHelper.toString(svgK.document());
    // Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }

  @Test
  public void testZonesTwoRows() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonRectangularKeyboardReader.class.getResource("Keyboard layout two rows.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(SvgKeyboardTests.class.getResource("Keyboard two rows.svg").toURI()));

    JsonRectangularRowKeyboard layout = JsonRectangularKeyboardReader.rowKeyboard(source);
    RectangularKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    SvgKeyboard svgK = SvgKeyboard.zonedFrom(physicalKeyboard);
    String svg = domHelper.toString(svgK.document());
    assertEquals(expected, svg);
  }

  @Test
  public void testMmmmPRepresentation() throws Exception {
    Document zoned = domHelper.asDocument(
        new StreamSource(SvgKeyboardTests.class.getResource("Keyboard two keys.svg").toString()));
    String expected = Files.readString(Path
        .of(SvgKeyboard.class.getResource("Keyboard two keys mmmm p representation.svg").toURI()));

    ImmutableMap<String, String> reprs = ImmutableMap.of("TAB", "mmmm", "AD01", "p");
    Document svgK = SvgKeyboard.using(zoned)
        .withRepresentations(k -> ImmutableList.of(Representation.fromString(reprs.get(k))));
    String svg = domHelper.toString(svgK);
    assertEquals(expected, svg);
  }

  @Test
  public void testTrivialRepresentationTwoRows() throws Exception {
    Document zoned = domHelper.asDocument(
        new StreamSource(SvgKeyboardTests.class.getResource("Keyboard two rows.svg").toString()));
    String expected = Files.readString(Path
        .of(SvgKeyboard.class.getResource("Keyboard two rows trivial representation.svg").toURI()));

    Document svgK = SvgKeyboard.using(zoned)
        .withRepresentations(k -> ImmutableList.of(Representation.fromString(k)));
    String svg = domHelper.toString(svgK);
    assertEquals(expected, svg);
  }

  @Test
  public void testSvgRepresentationTwoKeys() throws Exception {
    Document zoned = domHelper.asDocument(
        new StreamSource(SvgKeyboardTests.class.getResource("Keyboard two keys.svg").toString()));
    Document icon = domHelper.asDocument(
        new StreamSource(SvgKeyboardTests.class.getResource("Icon tab.svg").toString()));
    String expected = Files.readString(
        Path.of(SvgKeyboard.class.getResource("Keyboard two keys SVG representation.svg").toURI()));

    // https://www.svgrepo.com/svg/339792/arrows-horizontal too bold.
    // https://www.svgrepo.com/svg/309715/keyboard-tab too short.
    // https://www.svgrepo.com/svg/445840/keyboard-tab too bold?
    // https://www.svgrepo.com/svg/111811/transfer-arrows too bold.
    // https://www.svgrepo.com/svg/164104/transfer Thanks.
    Document svgK = SvgKeyboard.using(zoned).withRepresentations(ImmutableListMultimap.of("TAB",
        Representation.fromSvg(icon), "AD01", Representation.fromString("A"))::get);
    String svg = domHelper.toString(svgK);
    assertEquals(expected, svg);
  }

  @Test
  public void testRepresentationMany() throws Exception {
    Document zoned = domHelper.asDocument(
        new StreamSource(SvgKeyboardTests.class.getResource("Keyboard two keys.svg").toString()));
    String expected = Files.readString(Path
        .of(SvgKeyboardTests.class.getResource("Keyboard two keys representation.svg").toURI()));

    SvgKeyboard svgK = SvgKeyboard.using(zoned);
    CharSource kbMapSource = Resources
        .asCharSource(KeyboardMapTests.class.getResource("Two keys short"), StandardCharsets.UTF_8);
    KeyboardMap kbMap = SimpleSymbolsReader.read(kbMapSource);
    Document svgR = svgK.withRepresentations(
        VisibleKeyboardMap.from(kbMap, ImmutableMap.of()).representations()::get);
    String svg = domHelper.toString(svgR);
    assertEquals(expected, svg);
  }
}
