package io.github.oliviercailloux.keyboardd.keyboard.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JsonPhysicalRowKey (String xKeyName, double width) {
  @JsonCreator()
  public JsonPhysicalRowKey(@JsonProperty("xKeyName") String xKeyName,
      @JsonProperty("width") Double width) {
    this(xKeyName == null ? "" : xKeyName, width == null ? 1d : width);
  }
}
