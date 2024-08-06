package io.github.oliviercailloux.keyboardd.representable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeysymEntry;
import java.util.Map;

public class VisibleKeyboardMap implements XKeyNamesRepresenter {
  public static VisibleKeyboardMap from(ListMultimap<String, Representation> representations) {
    return new VisibleKeyboardMap(representations);
  }

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
              /*
               * http://www.unicode.org/faq/unsup_char.html suggests to use a font that has glyphs
               * for invisible characters in our case, so let’s stick to the easy representation.
               */
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

  private VisibleKeyboardMap(ListMultimap<String, Representation> representations) {
    this.representations = ImmutableListMultimap.copyOf(representations);
  }

  public ImmutableSet<String> names() {
    return representations.keySet();
  }

  @Override
  public ImmutableList<Representation> representations(String name) {
    return representations.get(name);
  }

  public ImmutableListMultimap<String, Representation> representations() {
    return representations;
  }
}
