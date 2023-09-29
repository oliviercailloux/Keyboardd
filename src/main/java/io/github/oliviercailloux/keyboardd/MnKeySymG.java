package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

public record MnKeySymG (String mnemonic, int code, Optional<Integer> unicode, String comment,
    boolean deprecated) {
  public MnKeySymG {
    checkArgument(unicode.isEmpty() || comment.equals(""));
    if (!comment.equals(""))
      checkArgument(!deprecated);
  }
}
