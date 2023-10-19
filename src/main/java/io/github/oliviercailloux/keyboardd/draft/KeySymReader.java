package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

/**
 * The header indicates that when multiple mnemonics map to a given code, only the first one should
 * be considered non deprecated. However, this seems contradictory with a part of the file which,
 * according to the comments, purposefully defines duplicated mnemonics (see for example XK_F27 and
 * XK_R7).
 * 
 * Similarly, the header indicates that when multiple mnemonics map to a given unicode point, only
 * the first one should be considered non deprecated. However, contrary to what the header further
 * states, not all of these are marked as such (see for example XK_Oslash and XK_Ooblique, compare
 * with XK_ETH and XK_Eth).
 * 
 * Thus, this class does not consider that sym codes or unicode points should correspond to a single
 * mnemonic.
 * 
 * This class considers that XK_Greek_IOTAdiaeresis, with code 1957 = 0x07a5 and unicode empty and
 * comment “old typo”, is deprecated, as there is also XK_Greek_IOTAdieresis mapping to the same
 * code and unicode point 938 = U+03AA = Ϊ and no comment.
 * 
 * With this modification, among non-deprecated values, we have that two entries with the same code
 * map to the same unicode point (present or absent).
 * 
 * Two pairs of mnemonics share a unicode point but different codes: #define XK_radical 0x08d6
 * U+221A SQUARE ROOT (in XK_TECHNICAL) and #define XK_squareroot 0x100221A U+221A SQUARE ROOT (in
 * XK_MATHEMATICAL); as well as #define XK_partialderivative 0x08ef U+2202 PARTIAL DIFFERENTIAL (in
 * XK_TECHNICAL) and #define XK_partdifferential 0x1002202 U+2202 PARTIAL DIFFERENTIAL (in
 * XK_MATHEMATICAL). This class patches those by assigning XK_squareroot 0x100221A to no unicode and
 * comment “2√”; and XK_partdifferential 0x1002202 to U+1D6DB MATHEMATICAL BOLD PARTIAL
 * DIFFERENTIAL.
 * 
 * With these two modifications, among non-deprecated values, we have that two entries with the same
 * present unicode point map to the same code.
 */
public class KeySymReader {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeySymReader.class);

  private static final Pattern P_XK_NO_COMMENT =
      Pattern.compile("^#define XK_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)$");
  private static final Pattern P_XK_COMMENT = Pattern.compile(
      "^#define XK_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* (?<comment>[^U][^\\*]*) \\*/$");
  private static final Pattern P_XK_UNICODE = Pattern.compile(
      "^#define XK_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* U\\+(?<unicode>[0-9a-fA-F]+) .*\\*/$");
  private static final Pattern P_XK_UNICODE_DEPRECATED = Pattern.compile(
      "^#define XK_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\*\\(U\\+(?<unicodeDeprecated>[0-9a-fA-F]+) [^\\*]*\\)\\*/$");
  private static final ImmutableSet<Pattern> PATTERNS =
      ImmutableSet.of(P_XK_NO_COMMENT, P_XK_COMMENT, P_XK_UNICODE, P_XK_UNICODE_DEPRECATED);

  public static ImmutableBiMap<String, MnKeySym> parseAndPatch() {
    CharSource keysymdef = Resources.asCharSource(KeySymReader.class.getResource("keysymdef.h"),
        StandardCharsets.UTF_8);
    try {
      ImmutableSet<MnKeySymG> syms = parseGToSet(keysymdef);
    return strip(patchG(syms));
    // return parse(source);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public static ImmutableBiMap<String, MnKeySymG> parseG() {
    CharSource keysymdef = Resources.asCharSource(KeySymReader.class.getResource("keysymdef.h"),
        StandardCharsets.UTF_8);
    try {
      return parseG(keysymdef);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public static ImmutableBiMap<String, MnKeySym> parse(CharSource keysymdef) throws IOException {
    ImmutableSet<MnKeySymG> syms = parseGToSet(keysymdef);
    return strip(syms);
  }

  private static ImmutableBiMap<String, MnKeySym> strip(ImmutableSet<MnKeySymG> syms) {
    ImmutableSet<MnKeySym> symsNotDepr = syms.stream().filter(s -> !s.deprecated())
        .map(s -> new MnKeySym(s.mnemonic(), s.code(), s.unicode(), s.comment()))
        .collect(ImmutableSet.toImmutableSet());
    {
      ImmutableSetMultimap<Integer, MnKeySym> byCode =
          Maps.toMap(symsNotDepr, MnKeySym::code).asMultimap().inverse();
      ImmutableSet<Integer> duplicateCodes = byCode.keySet().stream()
          .filter(c -> byCode.get(c).size() >= 2).collect(ImmutableSet.toImmutableSet());
      LOGGER.debug("Duplicate codes: {}.", duplicateCodes);
      ImmutableSet<Collection<MnKeySym>> nonUnique = byCode.asMap().values().stream()
          .filter(s -> s.stream().map(MnKeySym::unicode).distinct().count() != 1)
          .collect(ImmutableSet.toImmutableSet());
      nonUnique.stream().forEach(n -> LOGGER.warn("Non unique: {}.", n));
      verify(nonUnique.isEmpty());
    }
    {
      ImmutableSet<MnKeySym> symsWithUnicode = symsNotDepr.stream()
          .filter(s -> s.unicode().isPresent()).collect(ImmutableSet.toImmutableSet());
      ImmutableSetMultimap<Integer, MnKeySym> byUnicode =
          Maps.toMap(symsWithUnicode, s -> s.unicode().get()).asMultimap().inverse();
      ImmutableSet<Collection<MnKeySym>> nonUnique = byUnicode.asMap().values().stream()
          .filter(s -> s.stream().map(MnKeySym::code).distinct().count() != 1)
          .collect(ImmutableSet.toImmutableSet());
      nonUnique.stream().forEach(n -> LOGGER.warn("Non unique: {}.", n));
      verify(nonUnique.isEmpty());
    }

    return symsNotDepr.stream()
        .collect(ImmutableBiMap.toImmutableBiMap(MnKeySym::mnemonic, s -> s));
  }

  public static ImmutableBiMap<String, MnKeySymG> parseG(CharSource keysymdef) throws IOException {
    ImmutableSet<MnKeySymG> syms = parseGToSet(keysymdef);
    return syms.stream().collect(ImmutableBiMap.toImmutableBiMap(MnKeySymG::mnemonic, s -> s));
  }

  private static ImmutableSet<MnKeySymG> patchG(Set<MnKeySymG> syms) {
    LinkedHashSet<MnKeySymG> patching = new LinkedHashSet<>(syms);
    {
      boolean removed = patching
          .remove(new MnKeySymG("Greek_IOTAdiaeresis", 1957, Optional.empty(), "old typo", false));
      verify(removed);
      patching.add(new MnKeySymG("Greek_IOTAdiaeresis", 1957, Optional.empty(), "old typo", true));
    } 
    {
      boolean removed =
          patching.remove(new MnKeySymG("squareroot", 16785946, Optional.of(8730), "", false));
      verify(removed);
      patching.add(new MnKeySymG("squareroot", 16785946, Optional.empty(), "2√", true));
    }
    {
      boolean removed =
          patching.remove(new MnKeySymG("partdifferential", 
              16785922, Optional.of(8706), "", false));
      verify(removed);
      patching.add(new MnKeySymG("partdifferential", 16785922, Optional.of(120539), "", true));
    }
    return ImmutableSet.copyOf(patching);
  }

  private static ImmutableSet<MnKeySymG> parseGToSet(CharSource keysymdef) throws IOException {
    ImmutableList<String> lines = keysymdef.readLines();

    final ImmutableSet.Builder<MnKeySymG> keySymBuilder = new ImmutableSet.Builder<>();

    for (String line : lines) {
      Optional<Matcher> matcherOpt = ParseUtils.matcherOpt(line, PATTERNS);
      if (!matcherOpt.isPresent())
        continue;
      Matcher matcher = matcherOpt.orElseThrow(VerifyException::new);
      String name = matcher.group("name");
      String codeStr = matcher.group("code");
      int code = Integer.parseInt(codeStr, 16);
      if (matcher.pattern().equals(P_XK_NO_COMMENT)) {
        keySymBuilder.add(new MnKeySymG(name, code, Optional.empty(), "", false));
      } else if (matcher.pattern().equals(P_XK_COMMENT)) {
        String comment = matcher.group("comment");
        if (comment.equals("deprecated"))
          keySymBuilder.add(new MnKeySymG(name, code, Optional.empty(), "", true));
        else
          keySymBuilder.add(new MnKeySymG(name, code, Optional.empty(), comment, false));
      } else if (matcher.pattern().equals(P_XK_UNICODE)) {
        String unicodeStr = matcher.group("unicode");
        int unicode = Integer.parseInt(unicodeStr, 16);
        keySymBuilder.add(new MnKeySymG(name, code, Optional.of(unicode), "", false));
      } else if (matcher.pattern().equals(P_XK_UNICODE_DEPRECATED)) {
        String unicodeStr = matcher.group("unicodeDeprecated");
        int unicode = Integer.parseInt(unicodeStr, 16);
        keySymBuilder.add(new MnKeySymG(name, code, Optional.of(unicode), "", true));
      }
    }

    return keySymBuilder.build();
  }
}
