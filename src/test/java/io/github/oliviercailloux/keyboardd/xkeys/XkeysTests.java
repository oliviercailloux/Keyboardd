package io.github.oliviercailloux.keyboardd.xkeys;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

public class XkeysTests {
  @Test
  public void testLatest() throws Exception {
    ImmutableSet<IntStream> expectedCodes =
        Stream
            .of(rangeClosed(9, 92), rangeClosed(94, 256), rangeClosed(360, 450),
                rangeClosed(452, 454), rangeClosed(456, 459), rangeClosed(472, 493),
                rangeClosed(505, 514), rangeClosed(520, 550), rangeClosed(568, 569),
                rangeClosed(584, 597), rangeClosed(600, 601), rangeClosed(616, 657),
                rangeClosed(664, 693), rangeClosed(696, 701), rangeClosed(704, 708))
            .collect(ImmutableSet.toImmutableSet());
    int expectedAliasesSize = 100;
            
    Xkeys keys = Xkeys.latest();

    assertEquals(expectedCodes, keys.codes());
    
    assertEquals(expectedCodes.size(), keys.canonicals().size());
    assertTrue(keys.canonicals().contains("BKSL"));
    assertFalse(keys.canonicals().contains("AC12"));
    assertFalse(keys.canonicals().contains("Unkown"));
    
    assertEquals("LSGT", keys.canonical((short)94));
    assertEquals("BKSL", keys.canonical((short) 51));
    assertThrows(IllegalArgumentException.class, () -> keys.canonical((short) 7));
    
    assertEquals(keys.canonicals(), keys.codeByCanonical().keySet());
    assertEquals(expectedCodes, keys.codeByCanonical().values());
    
    assertEquals(expectedCodes, keys.canonicalByCode().keySet());
    assertEquals(keys.canonicals(), keys.canonicalByCode().values());
    
    assertEquals(expectedAliasesSize, keys.aliases().size());
    assertFalse(keys.aliases().contains("BKSL"));
    assertTrue(keys.aliases().contains("AC12"));
    assertFalse(keys.aliases().contains("Unknown"));
    
    assertEquals(ImmutableSet.of(), keys.aliases("LSGT"));
    assertEquals(ImmutableSet.of("AC12"), keys.aliases("BKSL"));
    assertThrows(IllegalArgumentException.class, () -> keys.aliases("AC12"));
    assertThrows(IllegalArgumentException.class, () -> keys.aliases("Unknown"));
    
    assertEquals(keys.aliases(), keys.canonicalByAlias().keySet());
    assertTrue(keys.canonicals().containsAll(keys.canonicalByAlias().values()));
    
    assertEquals(expectedCodes.size() + expectedAliasesSize, keys.names().size());
    assertTrue(keys.names().contains("LSGT"));
    assertTrue(keys.names().contains("BKSL"));
    assertTrue(keys.names().contains("AC12"));
    assertFalse(keys.names().contains("Unknown"));
    
    assertFalse(keys.isAlias("LSGT"));
    assertFalse(keys.isAlias("BKSL"));
    assertTrue(keys.isAlias("AC12"));
    assertFalse(keys.isAlias("Unknown"));
    
    assertEquals("LSGT", keys.canonical("LSGT"));
    assertEquals("BKSL", keys.canonical("BKSL"));
    assertEquals("BKSL", keys.canonical("AC12"));
    assertThrows(IllegalArgumentException.class, () -> keys.canonical("Unknown"));
    
    assertEquals(94, keys.code("LSGT"));
    assertEquals(49, keys.code("TLDE"));
    assertEquals(708, keys.code("I708"));
    assertEquals(51, keys.code("BKSL"));
    assertEquals(51, keys.code("AC12"));
    assertThrows(IllegalArgumentException.class, () -> keys.code("Unknown"));
    
    assertEquals(ImmutableSet.of("LSGT"), keys.names((short)94));
    assertEquals(ImmutableSet.of("BKSL", "AC12"), keys.names((short)51));
    assertThrows(IllegalArgumentException.class, () -> keys.names((short)7));
    
    assertEquals(keys.names(), keys.codeByName().keySet());
    assertEquals(expectedCodes, keys.codeByName().values());
  }
}
