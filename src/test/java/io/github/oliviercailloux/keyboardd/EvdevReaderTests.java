package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableBiMap;

public class EvdevReaderTests {
  @Test
  public void testEvdevRead() throws Exception {
    ImmutableBiMap<String, Integer> read = EvdevReader.parse();
    assertEquals(94, read.get("LSGT"));
    assertEquals(49, read.get("TLDE"));
    assertEquals(708, read.get("I708"));
  }
}
