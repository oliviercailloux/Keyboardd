package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.keyboardd.utils.ParseUtils;

/**
 * Reads data as found in a xkbcommon-keysyms.h file and returns a set of mnemonics.
 * 
 * <p>
 * Each set of {@link ParsedMnemonic} instances returned by this class satisfy the following
 * properties.
 * </p>
 * <ul>
 * <li>The mnemonics that correspond to a given keysym code correspond to at most one UCP.</li>
 * <li>All non deprecated and non specific mnemonics corresponding to a given UCP correspond to the
 * same keysym code, or exactly one of them corresponds to a keysym code in the range
 * UcpByCode#IMPLICIT_UCP_KEYSYM_CODES.</li>
 * <li>Each entry in the set represents one entry in the source. There is no lookup logic, thus, for
 * example, an entry in the source that has as comment “deprecated alias for oslash” will be parsed
 * as a deprecated mnemonic with no associated UCP: it does not lookup the relevant UCP from the
 * entry corresponding to the oslash mnemonic.</li>
 * <li>The canonical mnemonic associated to a given keysym code is the first one in iteration order
 * of the returned set that is not specific (and not deprecated, unless that makes the set of candidates empty).</li>
 * </ul>
 */
class KeysymReader {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeysymReader.class);

  /**
   * Reflects a single entry in the source.
   */
  public static record ParsedMnemonic (String mnemonic, int code, Optional<Integer> unicode,
      boolean deprecated, boolean specific, String comment) {
    public static ParsedMnemonic noComment(String mnemonic, int code) {
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), false, false, "");
    }

    public static ParsedMnemonic unicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), false, false, "");
    }

    public static ParsedMnemonic deprecatedUnicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), true, false, "");
    }

    public static ParsedMnemonic specificUnicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), false, true, "");
    }

    public static ParsedMnemonic deprecated(String mnemonic, int code) {
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), true, false, "");
    }

    public static ParsedMnemonic commented(String mnemonic, int code, String comment) {
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), false, false, comment);
    }

    public static ParsedMnemonic deprecatedComment(String mnemonic, int code, String comment) {
      checkArgument(comment.toLowerCase().startsWith("deprecated"));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), true, false, comment);
    }

    public ParsedMnemonic {
      checkArgument(!mnemonic.isEmpty());
      if (specific) {
        checkArgument(unicode.isPresent());
      }
      if (!comment.isEmpty()) {
        checkArgument(!comment.isBlank());
      }
    }
  }

  private static final Pattern P_XKB_NO_COMMENT =
      Pattern.compile("^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)$");
  private static final Pattern P_XKB_UNICODE_MORE_SPECIFIC = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\*<U\\+(?<unicodeSpecific>[0-9a-fA-F]+) [^\\*]*>\\*/$");
  private static final Pattern P_XKB_UNICODE_DEPRECATED = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\*\\(U\\+(?<unicodeDeprecated>[0-9a-fA-F]+) [^\\*]*\\)\\*/$");
  private static final Pattern P_XKB_COMMENT = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+) +/\\* +(?<comment>[^\\* ]+( +[^\\* ]+)*) *\\*/$");
  private static final Pattern P_XKB_COMMENT_ALIAS = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* ([aA]lias for |[sS]ame as XKB_KEY_)(?<alias>[^\\*]+) \\*/$");
  private static final Pattern P_XKB_COMMENT_UNICODE = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* U\\+(?<unicode>[0-9a-fA-F]+) .*\\*/$");
  private static final Pattern P_XKB_COMMENT_DEPRECATED = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* deprecated((, )| )?(?<commentRemaining>[^\\*]*) \\*/$");
  private static final Pattern P_XKB_COMMENT_DEPRECATED_ALIAS = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* deprecated alias for (?<aliasDeprecated>[^\\*]+) \\*/$");
  private static final ImmutableSet<Pattern> PATTERNS_START = ImmutableSet.of(P_XKB_NO_COMMENT,
      P_XKB_UNICODE_MORE_SPECIFIC, P_XKB_UNICODE_DEPRECATED, P_XKB_COMMENT);
  private static final ImmutableSet<Pattern> PATTERNS_COMMENTS =
      ImmutableSet.of(P_XKB_COMMENT_ALIAS, P_XKB_COMMENT_UNICODE, P_XKB_COMMENT_DEPRECATED);

  /**
   * Returns the latest version of the mnemonics, as included in this library. This will evolve with
   * the library.
   * 
   * @return the latest version of the mnemonics.
   */
  public static ImmutableSet<ParsedMnemonic> latest() {
    CharSource keysyms = Resources.asCharSource(
        KeysymReader.class.getResource("xkbcommon-keysyms - 238d13.h"), StandardCharsets.UTF_8);
    ImmutableSet<ParsedMnemonic> latest;
    try {
      latest = parse(keysyms);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
    return latest;
  }

  public static ImmutableSet<ParsedMnemonic> parse(CharSource keysyms) throws IOException {
    ImmutableList<String> lines = keysyms.readLines();

    final ImmutableSet.Builder<ParsedMnemonic> keysymBuilder = new ImmutableSet.Builder<>();

    for (String line : lines) {
      Optional<Matcher> matcherStartOpt = ParseUtils.matcherOpt(line, PATTERNS_START);
      if (!matcherStartOpt.isPresent())
        continue;
      Matcher matcherStart = matcherStartOpt.orElseThrow(VerifyException::new);
      ParsedMnemonic parsed = parseLine(matcherStart);
      keysymBuilder.add(parsed);
    }

    ImmutableSet<ParsedMnemonic> keysymsSet = keysymBuilder.build();
    check(keysymsSet);
    return keysymsSet;
  }

  private static ParsedMnemonic parseLine(Matcher matcherStart) {
    ParsedMnemonic parsed;
    String name = matcherStart.group("name");
    String codeStr = matcherStart.group("code");
    int code = Integer.parseInt(codeStr, 16);
    if (matcherStart.pattern().equals(P_XKB_NO_COMMENT)) {
      parsed = ParsedMnemonic.noComment(name, code);
    } else if (matcherStart.pattern().equals(P_XKB_UNICODE_MORE_SPECIFIC)) {
      String unicodeStr = matcherStart.group("unicodeSpecific");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = (ParsedMnemonic.specificUnicode(name, code, unicode));
    } else if (matcherStart.pattern().equals(P_XKB_UNICODE_DEPRECATED)) {
      String unicodeStr = matcherStart.group("unicodeDeprecated");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = (ParsedMnemonic.deprecatedUnicode(name, code, unicode));
    } else {
      verify(matcherStart.pattern().equals(P_XKB_COMMENT));
      String comment = matcherStart.group("comment");
      Optional<Matcher> matcherCommentsOpt =
          ParseUtils.matcherOpt(matcherStart.group(), PATTERNS_COMMENTS);
      if (!matcherCommentsOpt.isPresent()) {
        parsed = (ParsedMnemonic.commented(name, code, comment));
      } else {
        Matcher matcherComments = matcherCommentsOpt.orElseThrow(VerifyException::new);
        parsed = parseLineComments(name, code, comment, matcherComments);
      }
    }
    return parsed;
  }

  private static ParsedMnemonic parseLineComments(String name, int code, String comment,
      Matcher matcherComments) {
    ParsedMnemonic parsed;
    if (matcherComments.pattern().equals(P_XKB_COMMENT_ALIAS)) {
      parsed = (ParsedMnemonic.commented(name, code, comment));
    } else if (matcherComments.pattern().equals(P_XKB_COMMENT_UNICODE)) {
      String unicodeStr = matcherComments.group("unicode");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = ParsedMnemonic.unicode(name, code, unicode);
    } else {
      verify(matcherComments.pattern().equals(P_XKB_COMMENT_DEPRECATED));
      parsed = (ParsedMnemonic.deprecatedComment(name, code, comment));
    }
    return parsed;
  }

  private static void check(Set<ParsedMnemonic> mns) {
    /* The mnemonics that correspond to a given keysym code correspond to at most one UCP. */
    ImmutableSetMultimap<Integer, ParsedMnemonic> mnsByCode =
        mns.stream().collect(ImmutableSetMultimap.toImmutableSetMultimap(m -> m.code(), m -> m));
    for (int code : mnsByCode.keySet()) {
      ImmutableSet<ParsedMnemonic> mnsForCode = mnsByCode.get(code);
      ImmutableSet<Integer> ucpsAll = mnsForCode.stream().map(m -> m.unicode())
          .flatMap(Optional::stream).collect(ImmutableSet.toImmutableSet());

      checkArgument(ucpsAll.size() <= 1,
          "Code 0x%s, UCPs %s.".formatted(Integer.toHexString(code), ucpsAll.toString()));
    }

    /*
     * All non deprecated and non specific mnemonics corresponding to a given UCP correspond to the
     * same keysym code, or exactly one of them corresponds to a keysym code in the range
     * UcpByCode#IMPLICIT_UCP_KEYSYM_CODES.
     */
    ImmutableSetMultimap<Integer,
        ParsedMnemonic> mnsByUcp = mns.stream().filter(m -> !m.deprecated())
            .filter(m -> !m.specific()).filter(m -> m.unicode().isPresent())
            .collect(ImmutableSetMultimap.toImmutableSetMultimap(
                m -> m.unicode().orElseThrow(VerifyException::new), m -> m));
    for (int ucp : mnsByUcp.keySet()) {
      ImmutableSet<ParsedMnemonic> mnsForUcp = mnsByUcp.get(ucp);
      ImmutableSet<Integer> codesForUcp =
          mnsForUcp.stream().map(m -> m.code()).collect(ImmutableSet.toImmutableSet());
      verify(codesForUcp.size() >= 1);
      if (codesForUcp.size() >= 2) {
        ImmutableSet<Integer> implicitCodes =
            Sets.intersection(codesForUcp, UcpByCode.IMPLICIT_UCP_KEYSYM_CODES).immutableCopy();
        checkArgument(implicitCodes.size() == 1,
            "Ucp %s, mns %s.".formatted(ucp, mnsForUcp.toString()));
      }
    }
  }
}
