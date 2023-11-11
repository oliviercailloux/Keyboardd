package io.github.oliviercailloux.keyboardd.mnemonics;

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

import io.github.oliviercailloux.keyboardd.mnemonics.KeySymReader;
import io.github.oliviercailloux.keyboardd.mnemonics.KeySymReader.ParsedMnemonic;
import io.github.oliviercailloux.keyboardd.draft.MnKeySym;
import io.github.oliviercailloux.keyboardd.draft.MnKeySymG;

public class KeySymReaderTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeySymReaderTests.class);

  @Test
  public void testReadComment() throws Exception {
    CharSource source =
        CharSource.wrap("#define XKB_KEY_VoidSymbol  0xffffff  /* Void symbol */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.commented("VoidSymbol", Integer.parseInt("ffffff", 16), "Void symbol")), mns);
  }
  
    @Test
    public void testReadCommentQ() throws Exception {
      CharSource source =
          CharSource.wrap("#define XKB_KEY_XF86Option   0x1008ff6c  /* ?? */\n");
      ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
      assertEquals(ImmutableSet.of(ParsedMnemonic.commented("XF86Option", Integer.parseInt("1008ff6c", 16), "??")), mns);
    }

  @Test
  public void testReadCommentDont() throws Exception {
    CharSource source =
        CharSource.wrap("#define XKB_KEY_XF86RotationPB  0x1008ff75  /* don't use                   */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.commented("XF86RotationPB", Integer.parseInt("1008ff75", 16), "don't use")), mns);
  }

  @Test
  public void testReadCommentU() throws Exception {
    CharSource source =
        CharSource.wrap("#define XKB_KEY_XF86ScrollClick     0x1008ff7a  /* Use XKB mousekeys instead   */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.commented("XF86ScrollClick", Integer.parseInt("1008ff7a", 16), "Use XKB mousekeys instead")), mns);
  }

  @Test
  public void testReadNoComment() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_Scroll_Lock                   0xff14\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.noComment("Scroll_Lock", Integer.parseInt("ff14", 16))), mns);
  }

  @Test
  public void testReadUnicode() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_Tab   0xff09  /* U+0009 CHARACTER TABULATION */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.unicode("Tab", Integer.parseInt("ff09", 16), 9)), mns);
  }

  /** This fails if restricting the code regexp to lower case letters. */
  @Test
  public void testReadUnicodeUpperCase() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_containsas  0x100220B  /* U+220B CONTAINS AS MEMBER */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.unicode("containsas", Integer.parseInt("100220B", 16), Integer.parseInt("220B", 16))), mns);
  }

  /** This fails if restricting the code regexp to lower case letters. */
  @Test
  public void testReadUnicodeUpperCaseU() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_braille_dots_24  0x100280a  /* U+280a BRAILLE PATTERN DOTS-24 */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.unicode("braille_dots_24", Integer.parseInt("100280a", 16), Integer.parseInt("280a", 16))), mns);
  }

  @Test
  public void testReadUnicodeSpecific() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_KP_Space   0xff80  /*<U+0020 SPACE>*/\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.specificUnicode("KP_Space", Integer.parseInt("ff80", 16), Integer.parseInt("20", 16))), mns);
  }

  @Test
  public void testReadDeprecatedAlias() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_KP_Page_Up  0xff9a  /* deprecated alias for KP_Prior */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.deprecatedAlias("KP_Page_Up", Integer.parseInt("ff9a", 16), "deprecated alias for KP_Prior", "KP_Prior")), mns);
  }

  @Test
  public void testReadAlias() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_ISO_Group_Shift  0xff7e  /* Alias for Mode_switch */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.alias("ISO_Group_Shift", Integer.parseInt("ff7e", 16), "Alias for Mode_switch", "Mode_switch")), mns);
  }

  @Test
  public void testReadDeprecatedComment() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_dead_small_schwa  0xfe8a  /* deprecated, remove in 2025 */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.deprecatedComment("dead_small_schwa", Integer.parseInt("fe8a", 16), "deprecated, remove in 2025", ", remove in 2025")), mns);
  }

  @Test
  public void testReadDeprecatedCommentMissp() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_guillemotleft  0x00ab  /* deprecated misspelling */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.deprecatedComment("guillemotleft", Integer.parseInt("ab", 16), "deprecated misspelling", " misspelling")), mns);
  }

  @Test
  public void testReadDeprecated() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_quoteleft  0x0060  /* deprecated */\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.deprecated("quoteleft", Integer.parseInt("60", 16))), mns);
  }

  @Test
  public void testReadUnicodeDeprecated() throws Exception {
    CharSource source = CharSource.wrap("#define XKB_KEY_topleftradical  0x08a2  /*(U+250C BOX DRAWINGS LIGHT DOWN AND RIGHT)*/\n");
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.parse(source);
    assertEquals(ImmutableSet.of(ParsedMnemonic.deprecatedUnicode("topleftradical", Integer.parseInt("8a2", 16), Integer.parseInt("250C", 16))), mns);
  }

  @Test
  public void testLatest() throws Exception {
    ImmutableSet<ParsedMnemonic> mns = KeySymReader.latest();
    /** grep -c "^#define" "xkbcommon-keysyms - 238d13.h" */
    assertEquals(2573, mns.size());

    // ImmutableSet<MnKeySymG> noCommentsD = mns.stream()
    //     .filter(s -> s.unicode().isEmpty() && s.comment().equals("") && s.deprecated())
    //     .collect(ImmutableSet.toImmutableSet());
    // assertEquals(36, noCommentsD.size());

    // ImmutableSet<MnKeySymG> noCommentsNotD = symsG.values().stream()
    //     .filter(s -> s.unicode().isEmpty() && s.comment().equals("") && !s.deprecated())
    //     .collect(ImmutableSet.toImmutableSet());
    // assertEquals(302, noCommentsNotD.size());

    // ImmutableSet<MnKeySymG> comments = symsG.values().stream().filter(s -> !s.comment().equals(""))
    //     .collect(ImmutableSet.toImmutableSet());
    // assertEquals(124 - 36, comments.size());

    // ImmutableSet<MnKeySymG> unicode =
    //     symsG.values().stream().filter(s -> !s.unicode().isEmpty() && !s.deprecated())
    //         .collect(ImmutableSet.toImmutableSet());
    // assertEquals(1636, unicode.size());

    // ImmutableSet<MnKeySymG> unicodeD =
    //     symsG.values().stream().filter(s -> !s.unicode().isEmpty() && s.deprecated())
    //         .collect(ImmutableSet.toImmutableSet());
    // assertEquals(42, unicodeD.size());

    // assertEquals(2104, symsG.size());
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
