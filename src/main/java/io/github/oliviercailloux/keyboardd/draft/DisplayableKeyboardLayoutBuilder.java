package io.github.oliviercailloux.keyboardd.draft;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;

import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboard;
import io.github.oliviercailloux.keyboardd.keyboard.json.JsonPhysicalKeyboardReader;

public class DisplayableKeyboardLayoutBuilder {
  public static DisplayableKeyboardLayout build(CharSource layout, CharSource xkb)
      throws IOException {
    JsonPhysicalKeyboard l = JsonPhysicalKeyboardReader.parse().getLayout(layout);
    ImmutableSet<Key> keys = XkbReader.read(xkb);
    ImmutableMap<String, ImmutableList<KeyMapping>> keyValues =
        keys.stream().collect(ImmutableMap.toImmutableMap(k -> k.name(), k -> k.values()));
    ImmutableList<ImmutableList<JsonPhysicalKey>> rows = l.rows();
    for (ImmutableList<JsonPhysicalKey> row : rows) {
      final ImmutableList.Builder<DisplayableKey> newRow = new ImmutableList.Builder<>();
      for (JsonPhysicalKey key : row) {
        String name = key.name();
        ImmutableList<KeyMapping> values = keyValues.get(name);
      }
    }

  }
}
