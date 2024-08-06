package io.github.oliviercailloux.keyboardd.mapping;

import static com.google.common.base.Preconditions.checkArgument;

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
  }

  public static record Ucp (int ucp) implements KeysymEntry {
    public String asString() {
      /*
       * http://www.unicode.org/faq/unsup_char.html suggests to use a font that has glyphs
       * for invisible characters in our case, so let’s stick to the easy representation.
       */
      return new String(Character.toChars(ucp));
    }
  }

  public static record Code (int keysymCode) implements KeysymEntry {
  }
}
