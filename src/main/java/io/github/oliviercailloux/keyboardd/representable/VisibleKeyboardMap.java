package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Verify.verify;

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
          representation = defaultRepresentation(entry);
        }
        builder.put(xKeyName, representation);
      }
    }
    return new VisibleKeyboardMap(builder.build());
  }

  private static Representation defaultRepresentation(KeysymEntry entry) {
    return Representation.fromString(entry.asString());
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
