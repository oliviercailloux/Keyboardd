package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MoreCollectors;
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
 * <li>Among all mnemonics corresponding to a given keysym code in the returned set, exactly one is
 * canonical, and all others are aliases of that one.
 * <li>All mnemonics corresponding to a given keysym code in the returned set correspond to the same
 * UCP, or all correspond to no UCP.
 * </ul>
 * TODO check those.
 * 
 * old
 * 
 * <p>
 * This method patches the mnemonics to fix issue
 * <a href="https://github.com/xkbcommon/libxkbcommon/issues/433">#433</a>.
 * <p>
 * Multiple codes may may to a given ucp (eg mnemonic exclam, code 0x21, ucp U+0021 EXCLAMATION
 * MARK, and mnemonic absent, code 0x1000021, ucp U+0021).
 * 
 * Two pairs of mnemonics share a unicode point but different codes: radical, 0x08d6, U+221A SQUARE
 * ROOT (in Technical) and squareroot, 0x100221A, U+221A SQUARE ROOT; as well as partialderivative,
 * 0x08ef, U+2202 PARTIAL DIFFERENTIAL (in Technical) and partdifferential, 0x1002202, U+2202
 * PARTIAL DIFFERENTIAL (in XK_MATHEMATICAL). This class patches those by assigning squareroot,
 * 0x100221A, to no unicode and comment “2√”; and partdifferential, 0x1002202, to U+1D6DB
 * MATHEMATICAL BOLD PARTIAL DIFFERENTIAL.
 * 
 * With these two modifications, among non-deprecated values, we have that two entries with the same
 * present unicode point map to the same code.
 * 
 * /* Among all non-deprecated mns assigned to a given sym, if not empty [such as #define
 * XKB_KEY_topleftradical 0x08a2 /*(U+250C BOX DRAWINGS LIGHT DOWN AND RIGHT)], exactly one is not
 * an alias, and all others are aliases of that one.
 * 
 * Check: when mn1, mn2 to same code, then non first ones are either deprecated or comment equals
 * “alias for …”.
 */
class KeySymReader {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeySymReader.class);

  /**
   * Just a single line entry, thus, with no lookup logic, thus, “deprecated alias for oslash” does
   * not lookup for the oslash mnemonic (mapped to U+00F8), thus, does not retrieve the
   * corresponding UCP.
   */
  public static record ParsedMnemonic (String mnemonic, int code, Optional<Integer> unicode,
      String comment, boolean deprecated, boolean alias, boolean specific,
      String remainingComment) {
    public static ParsedMnemonic noComment(String mnemonic, int code) {
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), "", false, false, false, "");
    }

    public static ParsedMnemonic unicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), "", false, false, false, "");
    }

    public static ParsedMnemonic deprecatedUnicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), "", true, false, false, "");
    }

    public static ParsedMnemonic specificUnicode(String mnemonic, int code, int unicode) {
      return new ParsedMnemonic(mnemonic, code, Optional.of(unicode), "", false, false, true, "");
    }

    public static ParsedMnemonic deprecated(String mnemonic, int code) {
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), "deprecated", true, false, false,
          "");
    }

    public static ParsedMnemonic commented(String mnemonic, int code, String comment) {
      checkArgument(!comment.toLowerCase().contains("alias "));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), comment, false, false, false, "");
    }

    public static ParsedMnemonic alias(String mnemonic, int code, String comment, String aliasRef) {
      checkArgument(comment.toLowerCase().startsWith("alias for ")
          || comment.toLowerCase().startsWith("same as xkb_key_"));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), comment, false, true, false,
          aliasRef);
    }

    public static ParsedMnemonic deprecatedAlias(String mnemonic, int code, String comment,
        String aliasRef) {
      checkArgument(comment.toLowerCase().startsWith("deprecated alias for "));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), comment, true, true, false,
          aliasRef);
    }

    public static ParsedMnemonic deprecatedComment(String mnemonic, int code, String comment,
        String remainingComment) {
      checkArgument(comment.toLowerCase().startsWith("deprecated"));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), comment, true, false, false,
          remainingComment);
    }

    public ParsedMnemonic {
      checkArgument(!mnemonic.isEmpty());
      checkArgument(unicode.isEmpty() || comment.equals(""));
      if (specific) {
        checkArgument(unicode.isPresent());
      }
      checkArgument(comment.contains(remainingComment));
      if (alias) {
        checkArgument(!remainingComment.isEmpty());
      }
      if (!comment.isEmpty()) {
        checkArgument(!comment.isBlank());
      }
      if (!remainingComment.isEmpty()) {
        checkArgument(!remainingComment.isBlank());
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
    ImmutableSet<ParsedMnemonic> latestRaw = latestRaw();
    ImmutableSet<ParsedMnemonic> patched = patch(latestRaw);
    check(patched);
    return patched;
  }

  static ImmutableSet<ParsedMnemonic> latestRaw() {
    CharSource keysyms = Resources.asCharSource(
        KeySymReader.class.getResource("xkbcommon-keysyms - 238d13.h"), StandardCharsets.UTF_8);
    try {
      return parse(keysyms);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public static ImmutableSet<ParsedMnemonic> parse(CharSource keysyms) throws IOException {
    ImmutableList<String> lines = keysyms.readLines();

    final ImmutableSet.Builder<ParsedMnemonic> keySymBuilder = new ImmutableSet.Builder<>();

    for (String line : lines) {
      Optional<Matcher> matcherStartOpt = ParseUtils.matcherOpt(line, PATTERNS_START);
      if (!matcherStartOpt.isPresent())
        continue;
      Matcher matcherStart = matcherStartOpt.orElseThrow(VerifyException::new);
      ParsedMnemonic parsed = parseLine(matcherStart);
      keySymBuilder.add(parsed);

    }
    return keySymBuilder.build();
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
      String aliasRef = matcherComments.group("alias");
      parsed = (ParsedMnemonic.alias(name, code, comment, aliasRef));
    } else if (matcherComments.pattern().equals(P_XKB_COMMENT_UNICODE)) {
      String unicodeStr = matcherComments.group("unicode");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = ParsedMnemonic.unicode(name, code, unicode);
    } else {
      verify(matcherComments.pattern().equals(P_XKB_COMMENT_DEPRECATED));
      String remaining = matcherComments.group("commentRemaining");
      Matcher matcherCommentsDeprecated =
          P_XKB_COMMENT_DEPRECATED_ALIAS.matcher(matcherComments.group());
      if (matcherCommentsDeprecated.matches()) {
        String aliasRef = matcherCommentsDeprecated.group("aliasDeprecated");
        parsed = (ParsedMnemonic.deprecatedAlias(name, code, comment, aliasRef));
      } else {
        parsed = (ParsedMnemonic.deprecatedComment(name, code, comment, remaining));
      }
    }
    return parsed;
  }

  static ImmutableSet<ParsedMnemonic> patch(Set<ParsedMnemonic> mns) {
    final ImmutableMap.Builder<ParsedMnemonic, ParsedMnemonic> transformerBuilder =
        new ImmutableMap.Builder<>();
    transformerBuilder.put(ParsedMnemonic.commented("Kanji_Bangou", 65335, "Codeinput"),
        ParsedMnemonic.alias("Kanji_Bangou", 65335, "Alias for Codeinput", "Codeinput"));
    transformerBuilder.put(
        ParsedMnemonic.commented("Hangul_Codeinput", 65335, "Hangul code input mode"),
        ParsedMnemonic.alias("Hangul_Codeinput", 65335, "Alias for Codeinput", "Codeinput"));
    transformerBuilder.put(
        ParsedMnemonic.commented("Hangul_SingleCandidate", 65340, "Single candidate"),
        ParsedMnemonic.alias("Hangul_SingleCandidate", 65340, "Alias for SingleCandidate",
            "SingleCandidate"));
    transformerBuilder.put(ParsedMnemonic.commented("Zen_Koho", 65341, "Multiple/All Candidate(s)"),
        ParsedMnemonic.alias("Zen_Koho", 65341, "Alias for MultipleCandidate",
            "MultipleCandidate"));
    transformerBuilder.put(
        ParsedMnemonic.commented("Hangul_MultipleCandidate", 65341, "Multiple candidate"),
        ParsedMnemonic.alias("Hangul_MultipleCandidate", 65341, "Alias for MultipleCandidate",
            "MultipleCandidate"));
    transformerBuilder.put(ParsedMnemonic.commented("Mae_Koho", 65342, "Previous Candidate"),
        ParsedMnemonic.alias("Mae_Koho", 65342, "Alias for PreviousCandidate",
            "PreviousCandidate"));
    transformerBuilder.put(
        ParsedMnemonic.commented("Hangul_PreviousCandidate", 65342, "Previous candidate"),
        ParsedMnemonic.alias("Hangul_PreviousCandidate", 65342, "Alias for PreviousCandidate",
            "PreviousCandidate"));
    ImmutableMap<ParsedMnemonic, ParsedMnemonic> transformer = transformerBuilder.build();

    ImmutableSet<ParsedMnemonic> transformed = mns.stream().map(m -> transformer.getOrDefault(m, m))
        .collect(ImmutableSet.toImmutableSet());
    return transformed;
  }

  static void check(Set<ParsedMnemonic> mns) {
    ImmutableSetMultimap<Integer, ParsedMnemonic> mnsByCode =
        mns.stream().collect(ImmutableSetMultimap.toImmutableSetMultimap(m -> m.code, m -> m));
    for (int code : mnsByCode.keySet()) {
      ImmutableSet<ParsedMnemonic> mnsForCode = mnsByCode.get(code);
      ImmutableSet<ParsedMnemonic> nonDeprs =
          mnsForCode.stream().filter(m -> !m.deprecated).collect(ImmutableSet.toImmutableSet());
      if (nonDeprs.isEmpty()) {
        // LOGGER.info("No non deprecated for code {}.", code);
        continue;
      }
      ImmutableSet<ParsedMnemonic> nonAliases =
          nonDeprs.stream().filter(m -> !m.alias).collect(ImmutableSet.toImmutableSet());
      // if (nonAliases.size() != 1) {
      // LOGGER.info("Non aliases: {}.", nonAliases);
      // continue;
      // }
      verify(nonAliases.size() == 1, nonAliases.toString());
      ParsedMnemonic nonAlias = nonAliases.stream().collect(MoreCollectors.onlyElement());
      ImmutableSet<ParsedMnemonic> aliases =
          nonDeprs.stream().filter(m -> m.alias).collect(ImmutableSet.toImmutableSet());
      verify(aliases.size() == nonDeprs.size() - 1);
      for (ParsedMnemonic alias : aliases) {
        verify(alias.remainingComment.equals(nonAlias.mnemonic),
            "Alias %s for %s.".formatted(alias, nonAlias.mnemonic));
      }

      /* Each sym has only ucps or none, and all the same. */
      ImmutableSet<Optional<Integer>> ucpsAll = mnsForCode.stream().filter(m -> !m.deprecated())
          .filter(m -> !m.alias()).map(m -> m.unicode()).collect(ImmutableSet.toImmutableSet());

      verify(ucpsAll.size() <= 1,
          "Code 0x%s, unics %s.".formatted(Integer.toHexString(code), ucpsAll.toString()));
    }

    /*
     * Each unicode …? The test fails for the KP mnemonics if including specific ones: KP_Add to
     * KP_Divide, KP_0 to KP_9, KP_Return, KP_Tab, KP_Space, KP_Equal, KP_Separator, KP_Decimal. The
     * test fails for many mnemonics if including deprecated ones: period and decimalpoint, less and
     * leftcaret, underscore and underbar, macron and overbar, topleftradical and upleftcorner,
     * horizconnector and horizlinescan5, includedin and leftshoe, … Anyway, this is hopeless, I
     * suppose, as any ucp is automatically mapped to a code, which, I suppose, differs very often
     * from the mnemonic one. I’d better assume (reasonably, I suppose) that any X system will do
     * the same thing when facing two keysym codes that are standardly mapped to the same unicode
     * (such as the mnemonic “exclam” with keysym code 0x21 and the mnemonic absent with keysym code
     * 0x1000021 corresponding to U+0021 EXCLAMATION MARK), and thus not try to make ucps
     * distinguish these keysym codes.
     */
    ImmutableSetMultimap<Integer,
        ParsedMnemonic> mnsByUcp = mns.stream().filter(m -> !m.deprecated())
            .filter(m -> !m.specific()).filter(m -> m.unicode().isPresent())
            .collect(ImmutableSetMultimap.toImmutableSetMultimap(
                m -> m.unicode().orElseThrow(VerifyException::new), m -> m));
    for (int ucp : mnsByUcp.keySet()) {
      ImmutableSet<ParsedMnemonic> mnsForUcp = mnsByUcp.get(ucp);
      if (mnsForUcp.size() != 1) {
        LOGGER.info("Ucp {} mns {}.", ucp, mnsForUcp);
      }
      // verify(mnsForUcp.size() == 1, "Ucp %s, mns %s.".formatted(ucp, mnsForUcp.toString()));
    }
  }
}
