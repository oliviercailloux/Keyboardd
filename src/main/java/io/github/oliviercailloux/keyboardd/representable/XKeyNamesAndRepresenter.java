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

  static <V> XKeyNamesAndRepresenter fromIndirect(ListMultimap<String, V> toV,
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
  public ImmutableSet<String> names();

  public ImmutableListMultimap<String, Representation> representations();

  public static Representation defaultRepresentation(KeysymEntry entry) {
    return DefaultRepresentations.represent(entry);
  }

  public static Representation defaultRepresentation(CanonicalKeysymEntry entry) {
    return DefaultRepresentations.represent(entry);
  }
}
