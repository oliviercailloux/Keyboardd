package io.github.oliviercailloux.keyboardd;

import com.google.common.collect.ImmutableList;

public record DisplayableKey(double width, ImmutableList<String> displays) {
  
}
