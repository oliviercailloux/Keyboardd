package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
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

import io.github.oliviercailloux.keyboardd.draft.MnKeySym;
import io.github.oliviercailloux.keyboardd.utils.ParseUtils;
import io.github.oliviercailloux.keyboardd.xkeys.Xkeys;

/**
 * Among non-deprecated values, we have that two entries with the same code map to the same unicode
 * point (present or absent)?
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
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), "deprecated", true, false, false, "");
    }

    public static ParsedMnemonic commented(String mnemonic, int code, String comment) {
      checkArgument(!comment.toLowerCase().contains("alias "));
      return new ParsedMnemonic(mnemonic, code, Optional.empty(), comment, false, false, false, "");
    }

    public static ParsedMnemonic alias(String mnemonic, int code, String comment, String aliasRef) {
      checkArgument(comment.toLowerCase().startsWith("alias for "));
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
  private static final Pattern P_XKB_COMMENT_ALIAS = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* [aA]lias for (?<alias>[^\\*]+) \\*/$");
  private static final Pattern P_XKB_COMMENT_DEPRECATED_ALIAS = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* deprecated alias for (?<aliasDeprecated>[^\\*]+) \\*/$");
  private static final Pattern P_XKB_COMMENT_DEPRECATED = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* deprecated(?<commentRemaining>[^\\*]*) \\*/$");
  private static final Pattern P_XKB_COMMENT = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* (?<comment>[^U][^\\* ]*( [^\\* ]+)*) +\\*/$");
  private static final Pattern P_XKB_UNICODE = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\* U\\+(?<unicode>[0-9a-fA-F]+) .*\\*/$");
  private static final Pattern P_XKB_UNICODE_MORE_SPECIFIC = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\*<U\\+(?<unicodeSpecific>[0-9a-fA-F]+) [^\\*]*>\\*/$");
  private static final Pattern P_XKB_UNICODE_DEPRECATED = Pattern.compile(
      "^#define XKB_KEY_(?<name>[^ ]+) + 0x(?<code>[0-9a-fA-F]+)  /\\*\\(U\\+(?<unicodeDeprecated>[0-9a-fA-F]+) [^\\*]*\\)\\*/$");
  private static final ImmutableSet<Pattern> PATTERNS = ImmutableSet.of(P_XKB_NO_COMMENT,
      P_XKB_COMMENT, P_XKB_UNICODE, P_XKB_UNICODE_MORE_SPECIFIC, P_XKB_UNICODE_DEPRECATED);
  private static final ImmutableSet<Pattern> PATTERNS_COMMENTS =
      ImmutableSet.of(P_XKB_COMMENT_ALIAS, P_XKB_COMMENT_DEPRECATED_ALIAS);

  public static ImmutableSet<ParsedMnemonic> latest() {
    CharSource keysyms =
        Resources.asCharSource(KeySymReader.class.getResource("xkbcommon-keysyms - 238d13.h"), StandardCharsets.UTF_8);
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
      Optional<Matcher> matcherOpt = ParseUtils.matcherOpt(line, PATTERNS);
      if (!matcherOpt.isPresent())
        continue;
      Matcher matcher = matcherOpt.orElseThrow(VerifyException::new);
      ParsedMnemonic parsed = parseLine(matcher);
      keySymBuilder.add(parsed);

    }
    return keySymBuilder.build();
  }

  private static ParsedMnemonic parseLine(Matcher matcher) {
    ParsedMnemonic parsed;
    String name = matcher.group("name");
    String codeStr = matcher.group("code");
    int code = Integer.parseInt(codeStr, 16);
    if (matcher.pattern().equals(P_XKB_NO_COMMENT)) {
      parsed = ParsedMnemonic.noComment(name, code);
    } else if (matcher.pattern().equals(P_XKB_UNICODE)) {
      String unicodeStr = matcher.group("unicode");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = ParsedMnemonic.unicode(name, code, unicode);
    } else if (matcher.pattern().equals(P_XKB_UNICODE_DEPRECATED)) {
      String unicodeStr = matcher.group("unicodeDeprecated");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = (ParsedMnemonic.deprecatedUnicode(name, code, unicode));
    } else if (matcher.pattern().equals(P_XKB_UNICODE_MORE_SPECIFIC)) {
      String unicodeStr = matcher.group("unicodeSpecific");
      int unicode = Integer.parseInt(unicodeStr, 16);
      parsed = (ParsedMnemonic.specificUnicode(name, code, unicode));
    } else {
      verify(matcher.pattern().equals(P_XKB_COMMENT));
      String comment = matcher.group("comment");
      Optional<Matcher> matcherCOpt = ParseUtils.matcherOpt(matcher.group(), PATTERNS_COMMENTS);
      if (!matcherCOpt.isPresent()) {
        Matcher matcherSub = P_XKB_COMMENT_DEPRECATED.matcher(matcher.group());
        if (matcherSub.matches()) {
          String remaining = matcherSub.group("commentRemaining");
          parsed = (ParsedMnemonic.deprecatedComment(name, code, comment, remaining));
        } else {
          parsed = (ParsedMnemonic.commented(name, code, comment));
        }
      }
      else {
        Matcher matcherC = matcherCOpt.orElseThrow(VerifyException::new);
        if (matcherC.pattern().equals(P_XKB_COMMENT_ALIAS)) {
          String aliasRef = matcherC.group("alias");
          parsed = (ParsedMnemonic.alias(name, code, comment, aliasRef));
        } else if (matcherC.pattern().equals(P_XKB_COMMENT_DEPRECATED_ALIAS)) {
          String aliasRef = matcherC.group("aliasDeprecated");
          parsed = (ParsedMnemonic.deprecatedAlias(name, code, comment, aliasRef));
        } else {
          throw new VerifyException();
        }
      }
    }
    return parsed;
  }
}
