package io.github.oliviercailloux.keyboardd.draft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record KeyboardKey (String name, double size) {
  @JsonCreator()
  public KeyboardKey(@JsonProperty("name") String name, @JsonProperty("size") Double size) {
    this(name == null ? "" : name, size == null ? 1d : size);
  }
}
