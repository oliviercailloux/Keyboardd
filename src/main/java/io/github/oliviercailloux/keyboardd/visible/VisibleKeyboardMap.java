package io.github.oliviercailloux.keyboardd.visible;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import io.github.oliviercailloux.keyboardd.Representation;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeysymEntry;

public class VisibleKeyboardMap {
  public static VisibleKeyboardMap from(KeyboardMap keyboardMap,
      Map<KeysymEntry, Representation> representations) {
    ImmutableListMultimap.Builder<String, Representation> builder = ImmutableListMultimap.builder();
    for (String xKeyName : keyboardMap.names()) {
      for (KeysymEntry entry : keyboardMap.entries(xKeyName)) {
        Representation representation;
        if (representations.containsKey(entry)) {
          representation = representations.get(entry);
        } else {
          switch (entry.kind()) {
            case CODE:
              representation = Representation.fromString(entry.code().orElseThrow().toString());
              break;
            case MNEMONIC:
              representation = Representation.fromString(entry.mnemonic().orElseThrow());
              break;
            case UCP:
            /* http://www.unicode.org/faq/unsup_char.html suggests to use a font that has glyphs for invisible characters in our case, so let’s stick to the easy representation. */
              representation = Representation
                  .fromString(new String(Character.toChars(entry.ucp().orElseThrow())));
              break;
            default:
              throw new AssertionError();
          }
        }
        builder.put(xKeyName, representation);
      }
    }
    return new VisibleKeyboardMap(builder.build());
  }

  private final ImmutableListMultimap<String, Representation> representations;

  public VisibleKeyboardMap(ListMultimap<String, Representation> representations) {
    this.representations = ImmutableListMultimap.copyOf(representations);
  }

  public ImmutableList<Representation> representations(String xKeyName) {
    return representations.get(xKeyName);
  }
}
