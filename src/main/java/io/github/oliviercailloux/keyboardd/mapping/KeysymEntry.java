package io.github.oliviercailloux.keyboardd.mapping;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/** A keysym mnemonic (in the form of a `String`), a Unicode Code Point (an integer), or a keysym code (an integer), representing an entry in an XKB symbols file.  */
public class KeysymEntry {
  public enum Kind {
    MNEMONIC, UCP, CODE
  }

  public static KeysymEntry mnemonic(String keysymMnemonic) {
    return new KeysymEntry(keysymMnemonic, null, null);
  }

  public static KeysymEntry ucp(int ucp) {
    return new KeysymEntry(null, ucp, null);
  }

  public static KeysymEntry code(int keysymCode) {
    return new KeysymEntry(null, null, keysymCode);
  }

  private String mnemonic;
  private Integer ucp;
  private Integer code;

  private KeysymEntry(String keysymMnemonic, Integer ucp, Integer keysymCode) {
    int nbNulls = 0;
    if (keysymMnemonic == null)
      ++nbNulls;
    if (ucp == null)
      ++nbNulls;
    if (keysymCode == null)
      ++nbNulls;
    checkArgument(nbNulls == 2);
    mnemonic = keysymMnemonic;
    this.ucp = ucp;
    code = keysymCode;
  }

  public Kind kind() {
    if (mnemonic != null)
      return Kind.MNEMONIC;
    if (ucp != null)
      return Kind.UCP;
    assert code != null;
    return Kind.CODE;
  }
  
  public Optional<String> mnemonic() {
    return Optional.ofNullable(mnemonic);
  }

  public Optional<Integer> ucp() {
    return Optional.ofNullable(ucp);
  }

  public Optional<Integer> code() {
    return Optional.ofNullable(code);
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof KeysymEntry)) {
      return false;
    }
    final KeysymEntry t2 = (KeysymEntry) o2;
    return Objects.equals(mnemonic, t2.mnemonic) && Objects.equals(ucp, t2.ucp)
        && Objects.equals(code, t2.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mnemonic, ucp, code);
  }

  @Override
  public String toString() {
    ToStringHelper h = MoreObjects.toStringHelper(this);
    mnemonic().ifPresent(m -> h.add("Keysym mnemonic", m));
    ucp().ifPresent(u -> h.add("UCP", u));
    code().ifPresent(c -> h.add("Keysym code", c));
    return h.toString();
  }
}
