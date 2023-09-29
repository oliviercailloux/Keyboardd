package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;


public class KeyboardReaderTests {
  @Test
  public void testKey() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    
    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key name size.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      KeyboardKey k = mapper.readValue(r, KeyboardKey.class);
      assertEquals(new KeyboardKey("Ploum", 1.5d), k);
    }
  }

  @Test
  public void testKeyName() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key name.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      KeyboardKey k = mapper.readValue(r, KeyboardKey.class);
      assertEquals(new KeyboardKey("Ploum", 1d), k);
    }
  }

  @Test
  public void testRow() throws Exception {}
}
