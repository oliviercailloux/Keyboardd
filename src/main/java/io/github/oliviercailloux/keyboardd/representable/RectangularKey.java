package io.github.oliviercailloux.keyboardd.representable;

import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

/**
 * A key as physical object in a keyboard, with (optionnally) the signal that it sends.
 * <p>
 * The unit is 1 cm.
 */
public record RectangularKey (DoublePoint topLeftCorner, PositiveSize size,
    /* empty for no signal */ String xKeyName) {
  public static RectangularKey from(DoublePoint topLeftCorner, PositiveSize size, String xKeyName) {
    return new RectangularKey(topLeftCorner, size, xKeyName);
  }
}
