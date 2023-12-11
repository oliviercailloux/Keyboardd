package io.github.oliviercailloux.keyboardd.mnemonics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.keyboardd.mnemonics.Mnemonics.CanonicalMnemonic;

public class MnemonicsTests {
 @SuppressWarnings("unused")
 private static final Logger LOGGER = LoggerFactory.getLogger(MnemonicsTests.class);
 
  @Test
  public void testLatest() throws Exception {
    Mnemonics latest = Mnemonics.latest();
    LOGGER.debug("Keys: {}.", latest.asSet());
    ImmutableSet<String> mnemonics = latest.mnemonics();
    assertFalse(mnemonics.contains("NOT THERE"));
    assertTrue(mnemonics.contains("VoidSymbol"));
    assertTrue(mnemonics.contains("A"));
    assertTrue(mnemonics.contains("Page_Up"));
    assertTrue(mnemonics.contains("script_switch"));
    
    ImmutableSet<String> canonicals = latest.canonicals();
    assertFalse(canonicals.contains("NOT THERE"));
    assertTrue(canonicals.contains("VoidSymbol"));
    assertTrue(canonicals.contains("A"));
    assertFalse(canonicals.contains("Page_Up"));
    assertFalse(canonicals.contains("script_switch"));
    
    assertFalse(latest.isDeprecated("VoidSymbol"));
    CanonicalMnemonic prior = latest.asCanonicalMap().get("Prior");
    assertEquals(ImmutableSet.of("Page_Up"), prior.deprecatedAliases());
    assertTrue(latest.isDeprecated("Page_Up"));
    assertTrue(latest.isDeprecated("dead_small_schwa"));

    assertEquals(ImmutableSet.of(), latest.aliases("A"));
    assertThrows(IllegalArgumentException.class, () -> latest.aliases("Page_Up"));
    assertThrows(IllegalArgumentException.class, () -> latest.aliases("script_switch"));
    assertEquals(ImmutableSet.of("SunPageUp", "Page_Up"), latest.aliases("Prior"));
    // assertEquals(ImmutableSet.of("script_switch"), latest.aliases("Mode_switch"));

    assertEquals(ImmutableSet.of(), latest.nonDeprecatedAliases("A"));
    assertEquals(ImmutableSet.of("SunPageUp"), latest.nonDeprecatedAliases("Prior"));
    // assertEquals(ImmutableSet.of(), latest.nonDeprecatedAliases("Mode_switch"));
    
    ImmutableMap<String, Integer> codeByMnemonic = latest.codeByMnemonic();
    assertEquals(Integer.parseInt("FF08", 16), codeByMnemonic.get("BackSpace"));
    assertEquals(Integer.parseInt("20", 16), codeByMnemonic.get("space"));
    assertEquals(32, codeByMnemonic.get("space"));
    assertEquals(Integer.parseInt("41", 16), codeByMnemonic.get("A"));
    assertEquals(Integer.parseInt("1000174", 16), codeByMnemonic.get("Wcircumflex"));
    
    ImmutableMap<String, Integer> ucpByMnemonic = latest.ucpByMnemonic();
    assertFalse(ucpByMnemonic.containsKey("VoidSymbol"));
    assertEquals(' ', ucpByMnemonic.get("space"));
    assertEquals(' ', ucpByMnemonic.get("KP_Space"));
    assertEquals('A', ucpByMnemonic.get("A"));
    assertEquals('Ŵ', ucpByMnemonic.get("Wcircumflex"));
  }
 
  @Test
  public void testLatestNonDepr() throws Exception {
    Mnemonics latest = Mnemonics.latest().withoutDeprecated();
    LOGGER.debug("Keys: {}.", latest.asSet());
    ImmutableSet<String> mnemonics = latest.mnemonics();
    assertFalse(mnemonics.contains("NOT THERE"));
    assertTrue(mnemonics.contains("VoidSymbol"));
    assertTrue(mnemonics.contains("A"));
    assertTrue(mnemonics.contains("Prior"));
    assertFalse(mnemonics.contains("Page_Up"));
    assertTrue(mnemonics.contains("script_switch"));
    
    ImmutableSet<String> canonicals = latest.canonicals();
    assertFalse(canonicals.contains("NOT THERE"));
    assertTrue(canonicals.contains("VoidSymbol"));
    assertTrue(canonicals.contains("A"));
    assertFalse(canonicals.contains("Page_Up"));
    assertFalse(canonicals.contains("script_switch"));
    
    assertFalse(latest.isDeprecated("VoidSymbol"));
    assertThrows(IllegalArgumentException.class, () -> latest.isDeprecated("Page_Up"));
    assertThrows(IllegalArgumentException.class, () -> latest.isDeprecated("dead_small_schwa"));

    assertEquals(ImmutableSet.of(), latest.aliases("A"));
    assertThrows(IllegalArgumentException.class, () -> latest.aliases("Page_Up"));

    assertEquals(ImmutableSet.of(), latest.nonDeprecatedAliases("A"));
    assertThrows(IllegalArgumentException.class, () -> latest.nonDeprecatedAliases("Page_Up"));
    
    ImmutableMap<String, Integer> codeByMnemonic = latest.codeByMnemonic();
    assertEquals(Integer.parseInt("FF08", 16), codeByMnemonic.get("BackSpace"));
    assertEquals(Integer.parseInt("20", 16), codeByMnemonic.get("space"));
    assertEquals(32, codeByMnemonic.get("space"));
    assertEquals(Integer.parseInt("41", 16), codeByMnemonic.get("A"));
    assertEquals(Integer.parseInt("1000174", 16), codeByMnemonic.get("Wcircumflex"));
    
    ImmutableMap<String, Integer> ucpByMnemonic = latest.ucpByMnemonic();
    assertFalse(ucpByMnemonic.containsKey("VoidSymbol"));
    assertEquals(' ', ucpByMnemonic.get("space"));
    assertEquals(' ', ucpByMnemonic.get("KP_Space"));
    assertEquals('A', ucpByMnemonic.get("A"));
    assertEquals('Ŵ', ucpByMnemonic.get("Wcircumflex"));
  }
 
}
