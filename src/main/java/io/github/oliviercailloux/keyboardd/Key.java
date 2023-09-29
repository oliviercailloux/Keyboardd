package io.github.oliviercailloux.keyboardd;

import com.google.common.collect.ImmutableList;

public record Key(String name, ImmutableList<KeyMapping> values) {
  
}
