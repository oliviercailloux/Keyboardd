package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class KeyboardReaderTests {
  @Test
  public void testKey() throws Exception {
    Jsonb jsonb = JsonbBuilder.create();
    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key name size.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      KeyboardKey k = jsonb.fromJson(r, KeyboardKey.class);
      assertEquals(new KeyboardKey("Ploum", 1.5d), k);
    }
  }

  @Test
  public void testKeyName() throws Exception {
    Jsonb jsonb = JsonbBuilder.create();
    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key name.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      KeyboardKey k = jsonb.fromJson(r, KeyboardKey.class);
      assertEquals(new KeyboardKey("Ploum", 1d), k);
    }
  }

  @Test
  public void testRow() throws Exception {}
}
