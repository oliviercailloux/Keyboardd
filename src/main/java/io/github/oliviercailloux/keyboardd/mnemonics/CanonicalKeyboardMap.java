package io.github.oliviercailloux.keyboardd.mnemonics;

import com.google.common.collect.ImmutableList;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;

public class CanonicalKeyboardMap {
  public static CanonicalKeyboardMap canonicalize(KeyboardMap map, Mnemonics mnemonics) {
    return new CanonicalKeyboardMap(map, mnemonics);
  }

  private final KeyboardMap map;
  private final Mnemonics mnemonics;

  private CanonicalKeyboardMap(KeyboardMap map, Mnemonics mnemonics) {
    this.map = map;
    this.mnemonics = mnemonics;
  }

  public ImmutableList<CanonicalKeysymEntry> entries(String xKeyName) {
    return map.entries(xKeyName).stream().map(entry -> mnemonics.canonicalize(entry)).collect(ImmutableList.toImmutableList());
  }
}
