package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public class KeyboardReaderTests {
  @Test
  public void testKey() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(
        XkbReaderTests.class.getResource("Key name size.json"), StandardCharsets.UTF_8);
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
  public void testKeySize() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key size.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      KeyboardKey k = mapper.readValue(r, KeyboardKey.class);
      assertEquals(new KeyboardKey("", 1.5d), k);
    }
  }

  @Test
  public void testRow() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(XkbReaderTests.class.getResource("Key row.json"),
        StandardCharsets.UTF_8);
    JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, KeyboardKey.class);
    try (Reader r = source.openStream()) {
      List<KeyboardKey> ks = mapper.readValue(r, type);
      ImmutableList<KeyboardKey> row = ImmutableList.of(new KeyboardKey("Name1", 1.5d),
          new KeyboardKey("Name2", 1d), new KeyboardKey("", 1.5d));
      assertEquals(row, ks);
    }
  }

  @Test
  public void testLayout() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(
        XkbReaderTests.class.getResource("Keyboard layout unknown mnemonics.json"),
        StandardCharsets.UTF_8);
    JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, KeyboardKey.class);
    JavaType type2 = mapper.getTypeFactory().constructCollectionType(List.class, type);
    try (Reader r = source.openStream()) {
      List<List<KeyboardKey>> ks = mapper.readValue(r, type2);
      ImmutableList<KeyboardKey> row1 = ImmutableList.of(new KeyboardKey("Name1", 1.5d),
          new KeyboardKey("Name2", 1d), new KeyboardKey("", 1.5d));
      ImmutableList<KeyboardKey> row2 = ImmutableList.of(new KeyboardKey("Name3", 1.5d),
          new KeyboardKey("Name4", 1d), new KeyboardKey("", 1.5d));
      assertEquals(ImmutableList.of(row1, row2), ks);
    }
  }

  @Test
  public void testReadUnknownMnemonics() throws Exception {
    CharSource source = Resources.asCharSource(
        XkbReaderTests.class.getResource("Keyboard layout unknown mnemonics.json"),
        StandardCharsets.UTF_8);

    KeyboardLayoutBuilder parsed = KeyboardLayoutBuilder.parse();
    assertThrows(IllegalArgumentException.class, () -> parsed.getLayout(source));
  }

  @Test
  public void testReadSimple() throws Exception {
    CharSource source = Resources.asCharSource(
        XkbReaderTests.class.getResource("Keyboard layout simple.json"), StandardCharsets.UTF_8);

    KeyboardLayoutBuilder parsed = KeyboardLayoutBuilder.parse();
    KeyboardLayout layout = parsed.getLayout(source);
    ImmutableList<KeyboardKey> row =
        ImmutableList.of(new KeyboardKey("TAB", 1.5d), new KeyboardKey("AD01", 1d));
    assertEquals(ImmutableList.of(row), layout.rows());
  }

  @Test
  public void testReadFull() throws Exception {
    CharSource source = Resources.asCharSource(
        XkbReaderTests.class.getResource("Keyboard layout full.json"), StandardCharsets.UTF_8);

    KeyboardLayoutBuilder parsed = KeyboardLayoutBuilder.parse();
    KeyboardLayout layout = parsed.getLayout(source);
    ImmutableList<ImmutableList<KeyboardKey>> rows = layout.rows();
    assertEquals(6, rows.size());
    ImmutableList<KeyboardKey> row0 = rows.get(0);
    assertEquals(16, row0.size());
  }
}
