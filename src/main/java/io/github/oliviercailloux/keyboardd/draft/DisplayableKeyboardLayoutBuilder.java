package io.github.oliviercailloux.keyboardd.draft;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;

public class DisplayableKeyboardLayoutBuilder {
  public static DisplayableKeyboardLayout build(CharSource layout, CharSource xkb) throws IOException {
    KeyboardLayout l = KeyboardLayoutBuilder.parse().getLayout(layout);
    ImmutableSet<Key> keys = XkbReader.read(xkb);
    ImmutableMap<String, ImmutableList<KeyMapping>> keyValues = keys.stream().collect(ImmutableMap.toImmutableMap(k -> k.name(), k -> k.values()));
    ImmutableList<ImmutableList<KeyboardKey>> rows = l.rows();
    for (ImmutableList<KeyboardKey> row : rows) {
      final ImmutableList.Builder<DisplayableKey> newRow = new ImmutableList.Builder<>();
      for (KeyboardKey key : row) {
        String name = key.name();
        ImmutableList<KeyMapping> values = keyValues.get(name);
      }
    }
    
  }
}
