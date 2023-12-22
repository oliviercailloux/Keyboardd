package io.github.oliviercailloux.keyboardd.keyboard.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public class KeyboardWriterTests {
  @Test
  public void testWriteFull() throws Exception {
    CharSource sourceJson = Resources.asCharSource(getClass().getResource("Keyboard layout full.json"),
        StandardCharsets.UTF_8);

    JsonPhysicalRowKeyboard sourceObj = JsonPhysicalKeyboardReader.jsonPhysicalKeyboard(sourceJson);
    String written = JsonPhysicalKeyboardWriter.toJsonString(sourceObj);
    assertEquals(sourceJson.read(), written);
  }
}
