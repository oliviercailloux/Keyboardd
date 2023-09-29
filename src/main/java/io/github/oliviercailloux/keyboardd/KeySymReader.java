package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.jaris.collections.CollectionUtils;

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
 * Thus, this class does not consider that sym codes or unicode points should correspond to a single mnemonic.
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

  public static ImmutableBiMap<String, MnKeySym> parse() {
    CharSource source = Resources.asCharSource(KeySymReader.class.getResource("keysymdef.h"),
        StandardCharsets.UTF_8);
    try {
      return parse(source);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public static ImmutableBiMap<String, MnKeySymG> parseG() {
    CharSource source = Resources.asCharSource(KeySymReader.class.getResource("keysymdef.h"),
        StandardCharsets.UTF_8);
    try {
      return parseG(source);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public static ImmutableBiMap<String, MnKeySym> parse(CharSource keysymdef) throws IOException {
    ImmutableSet<MnKeySymG> syms = parseGToSet(keysymdef);
    ImmutableSet<MnKeySym> symsNotDepr = syms.stream().filter(s -> !s.deprecated())
        .map(s -> new MnKeySym(s.mnemonic(), s.code(), s.unicode(), s.comment()))
        .collect(ImmutableSet.toImmutableSet());
    {
      ImmutableSetMultimap<Integer, MnKeySym> byCode =
          Maps.toMap(symsNotDepr, MnKeySym::code).asMultimap().inverse();
      ImmutableSet<Integer> duplicateCodes = byCode.keySet().stream().filter(c -> byCode.get(c).size() >= 2)
          .collect(ImmutableSet.toImmutableSet());
      LOGGER.debug("Duplicate codes: {}.", duplicateCodes);
    }
    return symsNotDepr.stream()
        .collect(ImmutableBiMap.toImmutableBiMap(MnKeySym::mnemonic, s -> s));
  }

  public static ImmutableBiMap<String, MnKeySymG> parseG(CharSource keysymdef) throws IOException {
    ImmutableSet<MnKeySymG> syms = parseGToSet(keysymdef);
    return syms.stream().collect(ImmutableBiMap.toImmutableBiMap(MnKeySymG::mnemonic, s -> s));
  }

  private static ImmutableSet<MnKeySymG> parseGToSet(CharSource keysymdef) throws IOException {
    ImmutableList<String> lines = keysymdef.readLines();

    final ImmutableSet.Builder<MnKeySymG> keySymBuilder = new ImmutableSet.Builder<>();

    for (String line : lines) {
      Optional<Matcher> matcherOpt = matcher(line);
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

  private static Optional<Matcher> matcher(String line) {
    final ImmutableSet.Builder<Matcher> matchersBuilder = new ImmutableSet.Builder<>();
    for (Pattern pattern : PATTERNS) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches())
        matchersBuilder.add(matcher);
    }
    ImmutableSet<Matcher> matchers = matchersBuilder.build();
    verify(matchers.size() <= 1);

    return matchers.stream().collect(MoreCollectors.toOptional());
  }
}
