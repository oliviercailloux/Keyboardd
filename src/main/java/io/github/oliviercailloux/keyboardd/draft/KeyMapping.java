package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

public record KeyMapping (Optional<Integer> unicode, Optional<MnKeySym> sym) {
  public static KeyMapping unicode(int unicode) {
    return new KeyMapping(Optional.of(unicode), Optional.empty());
  }

  public static KeyMapping sym(MnKeySym sym) {
    return new KeyMapping(sym.unicode(), Optional.of(sym));
  }

  public KeyMapping {
    checkArgument(unicode.isPresent() || sym.isPresent());
    if (sym.isPresent())
      checkArgument(sym.flatMap(MnKeySym::unicode).equals(unicode), unicode.toString() + sym);
  }
}
