package io.github.oliviercailloux.keyboardd.keyboard.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JsonPhysicalKey (String xKeyName, double width) {
  @JsonCreator()
  public JsonPhysicalKey(@JsonProperty("name") String xKeyName,
      @JsonProperty("width") Double width) {
    this(xKeyName == null ? "" : xKeyName, width == null ? 1d : width);
  }
}
