package io.github.oliviercailloux.keyboardd.mnemonics;

import java.util.Optional;

public sealed interface CanonicalKeysymEntry permits CanonicalMnemonic, ImplicitUcp {
  default Optional<Integer> probeUcp() {
    if (this instanceof ImplicitUcp ucp) {
      return Optional.of(ucp.ucp());
    }
    CanonicalMnemonic mnemonic = (CanonicalMnemonic) this;
    return mnemonic.ucp();
  }
}
