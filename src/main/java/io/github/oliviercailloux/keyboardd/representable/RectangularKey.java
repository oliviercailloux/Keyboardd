package io.github.oliviercailloux.keyboardd.representable;

import io.github.oliviercailloux.geometry.Point;

/**
 * A key as physical object in a keyboard, with (optionnally) the signal that it sends.
 * <p>
 * The unit is 1 cm.
 */
public record RectangularKey (Point topLeftCorner, Point size,
    /* empty for no signal */ String xKeyName) {
  public static RectangularKey from(Point topLeftCorner, Point size, String xKeyName) {
    return new RectangularKey(topLeftCorner, size, xKeyName);
  }
}
