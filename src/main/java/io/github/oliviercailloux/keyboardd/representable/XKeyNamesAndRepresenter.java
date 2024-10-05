package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeysymEntry;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeyboardMap;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeysymEntry;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalMnemonic;
import io.github.oliviercailloux.keyboardd.mnemonics.ImplicitUcp;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface XKeyNamesAndRepresenter extends XKeyNamesRepresenter {
  public static XKeyNamesAndRepresenter from(ListMultimap<String, Representation> representations) {
    return new VisibleKeyboardMapImpl(representations);
  }

  public static XKeyNamesAndRepresenter from(KeyboardMap keyboardMap,
      Function<KeysymEntry, Representation> representations) {
    return fromIndirect(keyboardMap.nameToEntries(), representations);
  }

  public static XKeyNamesAndRepresenter from(CanonicalKeyboardMap keyboardMap,
      Function<CanonicalKeysymEntry, Representation> representations) {
    return fromIndirect(keyboardMap.nameToEntries(), representations);
  }

  private static <V> XKeyNamesAndRepresenter fromIndirect(ListMultimap<String, V> toV,
      Function<V, Representation> toRepresentations) {
    ImmutableListMultimap.Builder<String, Representation> builder = ImmutableListMultimap.builder();
    for (String xKeyName : toV.keySet()) {
      for (V entry : toV.get(xKeyName)) {
        Representation representation = toRepresentations.apply(entry);
        builder.put(xKeyName, representation);
      }
    }
    return new VisibleKeyboardMapImpl(builder.build());
  }

  /** The ones having at least one representation. */
  ImmutableSet<String> names();

  ImmutableListMultimap<String, Representation> representations();

  public static Representation defaultRepresentation(KeysymEntry entry) {
    return Representation.fromString(entry.asString());
  }
  public static Representation defaultRepresentation(CanonicalKeysymEntry entry) {
    return Representation.fromString(defaultString(entry));
  }

  private static String defaultString(CanonicalKeysymEntry entry) {
    final String str;
    if (entry instanceof CanonicalMnemonic mnemonic) {
      Optional<Integer> ucp = mnemonic.ucp();
      if (ucp.isPresent()) {
        str = new KeysymEntry.Ucp(ucp.orElseThrow()).asString();
      } else {
        str = mnemonic.mnemonic();
      }
    } else {
      verify(entry instanceof ImplicitUcp);
      ImplicitUcp implicitUcp = (ImplicitUcp) entry;
      str = implicitUcp.asString();
    }
    return str;
  }
}
