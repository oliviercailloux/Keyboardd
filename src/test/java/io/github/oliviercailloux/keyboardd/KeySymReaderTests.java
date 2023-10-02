package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

public class KeySymReaderTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeySymReaderTests.class);

  @Test
  public void testReadComment() throws Exception {
    CharSource source =
        CharSource.wrap("#define XK_VoidSymbol                  0xffffff  /* Void symbol */\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  @Test
  public void testReadNoComment() throws Exception {
    CharSource source = CharSource.wrap("#define XK_Tab                           0xff09\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  @Test
  public void testReadUnicode() throws Exception {
    CharSource source =
        CharSource.wrap("#define XK_space                         0x0020  /* U+0020 SPACE */\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  @Test
  public void testReadUnicodeDeprecated() throws Exception {
    CharSource source = CharSource.wrap(
        "#define XK_topleftradical                0x08a2  /*(U+250C BOX DRAWINGS LIGHT DOWN AND RIGHT)*/\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  /* This fails if restricting the code regexp to lower case letters. */
  @Test
  public void testReadCap() throws Exception {
    CharSource source = CharSource
        .wrap("#define XK_containsas                 0x100220B  /* U+220B CONTAINS AS MEMBER */\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  /* This fails if restricting the unicode regexp to upper case letters. */
  @Test
  public void testReadUniCap() throws Exception {
    CharSource source = CharSource.wrap(
        "#define XK_braille_dots_24            0x100280a  /* U+280a BRAILLE PATTERN DOTS-24 */\n");
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG(source);
    assertEquals(1, symsG.size());
  }

  @Test
  public void testReadG() throws Exception {
    ImmutableMap<String, MnKeySymG> symsG = KeySymReader.parseG();

    ImmutableSet<MnKeySymG> noCommentsD = symsG.values().stream()
        .filter(s -> s.unicode().isEmpty() && s.comment().equals("") && s.deprecated())
        .collect(ImmutableSet.toImmutableSet());
    assertEquals(36, noCommentsD.size());

    ImmutableSet<MnKeySymG> noCommentsNotD = symsG.values().stream()
        .filter(s -> s.unicode().isEmpty() && s.comment().equals("") && !s.deprecated())
        .collect(ImmutableSet.toImmutableSet());
    assertEquals(302, noCommentsNotD.size());

    ImmutableSet<MnKeySymG> comments = symsG.values().stream().filter(s -> !s.comment().equals(""))
        .collect(ImmutableSet.toImmutableSet());
    assertEquals(124 - 36, comments.size());

    ImmutableSet<MnKeySymG> unicode =
        symsG.values().stream().filter(s -> !s.unicode().isEmpty() && !s.deprecated())
            .collect(ImmutableSet.toImmutableSet());
    assertEquals(1636, unicode.size());

    ImmutableSet<MnKeySymG> unicodeD =
        symsG.values().stream().filter(s -> !s.unicode().isEmpty() && s.deprecated())
            .collect(ImmutableSet.toImmutableSet());
    assertEquals(42, unicodeD.size());

    assertEquals(2104, symsG.size());
  }

  @Test
  public void testRead() throws Exception {
    ImmutableMap<String, MnKeySym> syms = KeySymReader.parseAndPatch();
    LOGGER.debug("Keys: {}.", syms.keySet());
    assertEquals(Integer.parseInt("FFFFFF", 16), syms.get("VoidSymbol").code());
    assertEquals(Integer.parseInt("FF08", 16), syms.get("BackSpace").code());
    assertEquals(Integer.parseInt("20", 16), syms.get("space").code());
    assertEquals(32, syms.get("space").code());
    assertEquals(Integer.parseInt("41", 16), syms.get("A").code());
    assertEquals(Integer.parseInt("1000174", 16), syms.get("Wcircumflex").code());
  }
}
