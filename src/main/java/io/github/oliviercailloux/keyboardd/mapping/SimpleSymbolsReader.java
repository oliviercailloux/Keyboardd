package io.github.oliviercailloux.keyboardd.mapping;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import io.github.oliviercailloux.keyboardd.utils.ParseUtils;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A reader of XKB symbols files, which produces a {@link KeyboardMap}. 
 * The reader is quite crude; it will read correctly only the simplest files.
*/
public class SimpleSymbolsReader {
  private static final Pattern P_COMMENT = Pattern.compile("^( *//.*)| *$");
  private static final Pattern P_OTHER = Pattern
      .compile("^(partial.*)|(xkb_symbols .*)|( *key.type.*)|( *name.*)|( *include .+)|(\\};)$");
  private static final Pattern P_KEY =
      Pattern.compile("^ *key[ \\t]+<(?<name>.+)>[ \\t]*\\{[ \\t]*\\[[ \\t]*(?<entries>.*[^ \\t])[ \\t]*\\][ \\t]*\\}[ \\t]*;[ \\t]*(//.*)?$");
  private static final Pattern P_UNICODE = Pattern.compile("U(?<unicode>[0-9a-fA-F]+)");
  private static final ImmutableSet<Pattern> PATTERNS =
      ImmutableSet.of(P_COMMENT, P_OTHER, P_KEY);

      /**
       * Reads a keyboard map from the given source.
       * @param source the XKB symbols file to read from
       * @return a (possibly empty) keyboard map
       * @throws IOException if an I/O error occurs
       */
  public static KeyboardMap read(CharSource source) throws IOException {
    ImmutableList<String> lines = source.readLines();

    final ImmutableListMultimap.Builder<String, KeysymEntry> keys = new ImmutableListMultimap.Builder<>();
    for (String line : lines) {
      Matcher matcher = ParseUtils.matcher(line, PATTERNS);
      if (matcher.pattern().equals(P_COMMENT)) {
        continue;
      } else if (matcher.pattern().equals(P_OTHER)) {
        continue;
      } else {
        verify(matcher.pattern().equals(P_KEY));
        String name = matcher.group("name");
        String entries = matcher.group("entries");
        final ImmutableList<KeysymEntry> values = parseEntries(entries);
        keys.putAll(name, values);
      }
    }
    ImmutableListMultimap<String, KeysymEntry> map = keys.build();
    return KeyboardMap.from(map);
  }

  private static ImmutableList<KeysymEntry> parseEntries(String entriesOneStr) {
    String[] split = entriesOneStr.split(", ?");
    ImmutableList<String> entriesMultStr = ImmutableList.copyOf(split);

    final ImmutableList.Builder<KeysymEntry> entries = new ImmutableList.Builder<>();
    for (String entryStr : entriesMultStr) {
      Matcher matcher = P_UNICODE.matcher(entryStr);
      if (matcher.matches()) {
        String uStr = matcher.group("unicode");
        int u = Integer.parseInt(uStr, 16);
        entries.add(KeysymEntry.ucp(u));
      } else {
        entries.add(KeysymEntry.mnemonic(entryStr));
      }
    }
    return entries.build();
  }

}
