package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

public record MnKeySym (String mnemonic, int code, Optional<Integer> unicode, String comment) {
  public MnKeySym {
    checkArgument(unicode.isEmpty() || comment.equals(""));
  }
  
}