package io.github.oliviercailloux.keyboardd.mapping;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XkbKeymapDecomposer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(XkbKeymapDecomposer.class);
  
  private static final Pattern SYMBOLS = Pattern.compile("^xkb_symbols \"(?<name>.+)\" \\{[\\n\\r]*(?<contents>([^\\{]*\\{[^\\}]*\\})*[^\\}]+)[\\n" + //
        "\\r]*\\};$", Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern P_OTHER = Pattern.compile(
      "^(default )?(partial.*)|(xkb_symbols .*)|( *key.type.*)|( *name.*)|( *include .+)|( *modifier_map.*)|(\\};)$");

  public static ImmutableMap<String, String> bySymbolsMap(CharSource source) throws IOException {
     Matcher matcher = SYMBOLS.matcher(source.read());
     final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
     
    while(matcher.find()) {
      String name = matcher.group("name");
      String symbols = matcher.group("contents");
      LOGGER.info("Found name: {}, symbols {}.", name, symbols.substring(0, 50));
      builder.put(name, symbols);
    }
    return builder.build();
  }
}
