package io.github.oliviercailloux.keyboardd.keyboard;

import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

/** A key as physical object in a keyboard, with the signal that it sends. */
public record PhysicalKey(DoublePoint topLeftCorner, PositiveSize size, String xKeyName) implements GeometricKey{
  public static PhysicalKey from(DoublePoint topLeftCorner, PositiveSize size, String xKeyName) {
    return new PhysicalKey(topLeftCorner, size, xKeyName);
  }
}