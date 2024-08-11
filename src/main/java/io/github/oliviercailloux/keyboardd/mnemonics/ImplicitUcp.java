package io.github.oliviercailloux.keyboardd.mnemonics;

import io.github.oliviercailloux.keyboardd.mapping.KeysymEntry;

public record ImplicitUcp (int ucp) implements CanonicalKeysymEntry {
  public static ImplicitUcp byUcp(int ucp) {
    return new ImplicitUcp(ucp);
  }

  public static ImplicitUcp byCode(int code) {
    return new ImplicitUcp(UcpByCode.IMPLICIT_UCP_BY_CODE.apply(code));
  }

  public int code() {
    return UcpByCode.CODE_BY_IMPLICIT_UCP.apply(ucp);
  }

  public String asString() {
    return new KeysymEntry.Ucp(ucp).asString();
  }
}
