package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKeyboard;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboardReader;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalRowKeyboard;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMapTests;
import io.github.oliviercailloux.keyboardd.mapping.SimpleSymbolsReader;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import io.github.oliviercailloux.keyboardd.representable.VisibleKeyboardMap;
import io.github.oliviercailloux.svgb.PositiveSize;

public class DrawerTests {
  @Test
  public void testDraw() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonPhysicalKeyboardReader.class.getResource("Keyboard layout simple.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(SvgKeyboard.class.getResource("Keyboard simple scale 2.svg").toURI()));

    JsonPhysicalRowKeyboard layout = JsonPhysicalKeyboardReader.rowKeyboard(source);
    PhysicalKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    SvgKeyboard svgK = SvgKeyboard.from(physicalKeyboard);
    String svg = DomHelper.domHelper().toString(svgK.getDocument());
    Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }
  @Test
  public void testReDraw() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonPhysicalKeyboardReader.class.getResource("Keyboard layout simple.json"),
        StandardCharsets.UTF_8);
    String expected = Files
        .readString(Path.of(SvgKeyboard.class.getResource("Keyboard simple scale 2.svg").toURI()));

    JsonPhysicalRowKeyboard layout = JsonPhysicalKeyboardReader.rowKeyboard(source);
    PhysicalKeyboard physicalKeyboard =
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
