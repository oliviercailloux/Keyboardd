package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.draft.Drawer;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboard;

public class DrawerTests {
  @Test
  public void testDraw() throws Exception {
    CharSource source = Resources.asCharSource(
        this.getClass().getResource("Keyboard layout simple.json"), StandardCharsets.UTF_8);
    String expected = Files.readString(
        Path.of(DrawerTests.class.getResource("Keyboard layout unknown mnemonics.json").toURI()));

    JsonPhysicalKeyboard layout = JsonPhysicalKeyboardReader.parse().getLayout(source);
    Document doc = Drawer.draw(layout);
    String svg = DomHelper.domHelper().toString(doc);
    Files.writeString(Path.of("out.svg"), svg);
    assertEquals(expected, svg);
  }
}
