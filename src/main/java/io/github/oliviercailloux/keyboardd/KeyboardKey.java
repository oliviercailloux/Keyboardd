package io.github.oliviercailloux.keyboardd;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;

public record KeyboardKey (String name, @JsonbNillable() double size) {
  
}
