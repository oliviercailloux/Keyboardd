package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;

public class XkbReader {
  private static final Pattern P_COMMENT = Pattern.compile("^( *//.*)| *$");
  private static final Pattern P_OTHER = Pattern
      .compile("^(partial.*)|(xkb_symbols .*)|( *key.type.*)|( *name.*)|( *include .+)|(\\};)$");
  private static final Pattern P_KEY =
      Pattern.compile("^ *key <(?<name>.+)> \\{ \\[ (?<values>.+) \\] \\};$");
  private static final Pattern P_UNICODE = Pattern.compile("U(?<unicode>[0-9a-fA-F]+)");

  private static final ImmutableMap<String, MnKeySym> symbols = KeySymReader.parse();

  public static ImmutableSet<Key> read(CharSource source) throws IOException {
    ImmutableList<String> lines = source.readLines();

    final ImmutableSet.Builder<Key> keys = new ImmutableSet.Builder<>();
    for (String line : lines) {
      final LineKind kind = kind(line);
      if (kind == LineKind.KEY) {
        Matcher matcher = P_KEY.matcher(line);
        matcher.matches();
        String name = matcher.group("name");
        String valuesStr = matcher.group("values");
        final ImmutableList<KeyMapping> values = parseValues(valuesStr);
        keys.add(new Key(name, values));
      }
    }
    return keys.build();
  }

  private static ImmutableList<KeyMapping> parseValues(String values) {
    String[] split = values.split(", ?");
    ImmutableList<String> valuesStr = ImmutableList.copyOf(split);

    final ImmutableList.Builder<KeyMapping> mappings = new ImmutableList.Builder<>();
    for (String value : valuesStr) {
      Matcher matcher = P_UNICODE.matcher(value);
      if (matcher.matches()) {
        String uStr = matcher.group("unicode");
        int u = Integer.parseInt(uStr, 16);
        mappings.add(KeyMapping.unicode(u));
        // } else if (value.length()==1){
        // final int valueInt = value.codePoints().boxed().collect(MoreCollectors.onlyElement());
        // valuesInts.add(valueInt);
      } else {
        checkArgument(symbols.containsKey(value), value);
        MnKeySym sym = symbols.get(value);
        mappings.add(KeyMapping.sym(sym));
      }
    }
    return mappings.build();
  }

  private static LineKind kind(String line) {
    final ImmutableSet.Builder<LineKind> kindsBuilder = new ImmutableSet.Builder<>();
    if (P_COMMENT.matcher(line).matches()) {
      kindsBuilder.add(LineKind.COMMENT);
    }
    if (P_OTHER.matcher(line).matches()) {
      kindsBuilder.add(LineKind.OTHER);
    }
    if (P_KEY.matcher(line).matches()) {
      kindsBuilder.add(LineKind.KEY);
    }
    ImmutableSet<LineKind> kinds = kindsBuilder.build();
    verify(kinds.size() <= 1);
    checkArgument(kinds.size() == 1, line);
    return Iterables.getOnlyElement(kinds);
  }
}
