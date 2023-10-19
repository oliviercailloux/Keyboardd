package io.github.oliviercailloux.keyboardd.draft;

import com.google.common.collect.ImmutableList;

public record Key(String name, ImmutableList<KeyMapping> values) {
  
}
