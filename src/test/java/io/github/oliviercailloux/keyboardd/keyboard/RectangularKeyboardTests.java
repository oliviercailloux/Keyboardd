package io.github.oliviercailloux.keyboardd.keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularKeyboardReader;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonRectangularRowKeyboard;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboardTests;
import io.github.oliviercailloux.svgb.PositiveSize;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class RectangularKeyboardTests {
  @Test
  public void testFull() throws Exception {
    CharSource source = Resources.asCharSource(
        JsonRectangularKeyboardReader.class.getResource("Keyboard layout full.json"),
        StandardCharsets.UTF_8);

    JsonRectangularRowKeyboard layout = JsonRectangularKeyboardReader.rowKeyboard(source);
    RectangularKeyboard physicalKeyboard =
        layout.toPhysicalKeyboard(PositiveSize.square(2d), PositiveSize.square(1d));
    assertEquals(PositiveSize.given(47d, 17d), physicalKeyboard.size());
  }
}
