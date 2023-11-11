package io.github.oliviercailloux.keyboardd.keyboard.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKeyboard;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

public class KeyboardReaderTests {
  @Test
  public void testKey() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(getClass().getResource("Key name size.json"),
        StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      JsonPhysicalKey k = mapper.readValue(r, JsonPhysicalKey.class);
      assertEquals(new JsonPhysicalKey("Ploum", 1.5d), k);
    }
  }

  @Test
  public void testKeyName() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source =
        Resources.asCharSource(getClass().getResource("Key name.json"), StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      JsonPhysicalKey k = mapper.readValue(r, JsonPhysicalKey.class);
      assertEquals(new JsonPhysicalKey("Ploum", 1d), k);
    }
  }

  @Test
  public void testKeySize() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source =
        Resources.asCharSource(getClass().getResource("Key size.json"), StandardCharsets.UTF_8);
    try (Reader r = source.openStream()) {
      JsonPhysicalKey k = mapper.readValue(r, JsonPhysicalKey.class);
      assertEquals(new JsonPhysicalKey("", 1.5d), k);
    }
  }

  @Test
  public void testRow() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source =
        Resources.asCharSource(getClass().getResource("Key row.json"), StandardCharsets.UTF_8);
    JavaType type =
        mapper.getTypeFactory().constructCollectionType(List.class, JsonPhysicalKey.class);
    try (Reader r = source.openStream()) {
      List<JsonPhysicalKey> ks = mapper.readValue(r, type);
      ImmutableList<JsonPhysicalKey> row = ImmutableList.of(new JsonPhysicalKey("Name1", 1.5d),
          new JsonPhysicalKey("Name2", 1d), new JsonPhysicalKey("", 1.5d));
      assertEquals(row, ks);
    }
  }

  @Test
  public void testLayout() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    CharSource source = Resources.asCharSource(
        getClass().getResource("Keyboard layout unknown mnemonics.json"), StandardCharsets.UTF_8);
    JavaType type =
        mapper.getTypeFactory().constructCollectionType(List.class, JsonPhysicalKey.class);
    JavaType type2 = mapper.getTypeFactory().constructCollectionType(List.class, type);
    try (Reader r = source.openStream()) {
      List<List<JsonPhysicalKey>> ks = mapper.readValue(r, type2);
      ImmutableList<JsonPhysicalKey> row1 = ImmutableList.of(new JsonPhysicalKey("Name1", 1.5d),
          new JsonPhysicalKey("Name2", 1d), new JsonPhysicalKey("", 1.5d));
      ImmutableList<JsonPhysicalKey> row2 = ImmutableList.of(new JsonPhysicalKey("Name3", 1.5d),
          new JsonPhysicalKey("Name4", 1d), new JsonPhysicalKey("", 1.5d));
      assertEquals(ImmutableList.of(row1, row2), ks);
    }
  }

  @Test
  public void testReadSimple() throws Exception {
    CharSource source = Resources.asCharSource(
        getClass().getResource("Keyboard layout simple.json"), StandardCharsets.UTF_8);

    JsonPhysicalKeyboard parsed = JsonPhysicalKeyboardReader.jsonPhysicalKeyboard(source);
    ImmutableList<JsonPhysicalKey> row =
        ImmutableList.of(new JsonPhysicalKey("TAB", 1.5d), new JsonPhysicalKey("AD01", 1d));
    assertEquals(ImmutableList.of(row), parsed.rows());
  }

  @Test
  public void testReadFull() throws Exception {
    CharSource source = Resources.asCharSource(getClass().getResource("Keyboard layout full.json"),
        StandardCharsets.UTF_8);

    JsonPhysicalKeyboard parsed = JsonPhysicalKeyboardReader.jsonPhysicalKeyboard(source);
    ImmutableList<ImmutableList<JsonPhysicalKey>> rows = parsed.rows();
    assertEquals(6, rows.size());
    ImmutableList<JsonPhysicalKey> row0 = rows.get(0);
    assertEquals(16, row0.size());
  }

  @Test
  public void testReadAsPhys() throws Exception {
    CharSource source = Resources.asCharSource(
        getClass().getResource("Keyboard layout two rows.json"), StandardCharsets.UTF_8);

    PhysicalKey r1k1 = PhysicalKey.from(DoublePoint.zero(), PositiveSize.given(3d, 2d), "R1K1");
    PhysicalKey r1k2 =
        PhysicalKey.from(DoublePoint.given(4d, 0d), PositiveSize.given(4.5d, 2d), "R1K2");
    PhysicalKey r2k1 =
        PhysicalKey.from(DoublePoint.given(0d, 3.2d), PositiveSize.given(6d, 2d), "R2K1");
    PhysicalKey r2k2 =
        PhysicalKey.from(DoublePoint.given(7d, 3.2d), PositiveSize.given(3d, 2d), "R2K2");
    PhysicalKeyboard expected = PhysicalKeyboard.from(ImmutableSet.of(r1k1, r1k2, r2k1, r2k2));

    PhysicalKeyboard keyboard = JsonPhysicalKeyboardReader.jsonPhysicalKeyboard(source)
        .toPhysicalKeyboard(PositiveSize.given(3d, 2d), PositiveSize.given(1d, 1.2d));
    assertEquals(expected, keyboard);
  }
}
