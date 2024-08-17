package io.github.oliviercailloux.keyboardd.mapping;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.Objects;
import java.util.Optional;

/**
 * A keysym mnemonic (in the form of a `String`), a Unicode Code Point (an integer), or a keysym
 * code (an integer), representing an entry in an XKB symbols file.
 */
public sealed interface KeysymEntry
    permits KeysymEntry.Mnemonic, KeysymEntry.Ucp, KeysymEntry.Code {

  public static record Mnemonic (String keysymMnemonic) implements KeysymEntry {
    @Override
    public String asString() {
      return keysymMnemonic();
    }
  }

  public static record Ucp (int ucp) implements KeysymEntry {
    @Override
    public String asString() {
      return new String(Character.toChars(ucp));
    }
  }

  public static record Code (int keysymCode) implements KeysymEntry {
    @Override
    public String asString() {
      return String.valueOf(keysymCode());
    }
  }

  String asString();
}
