package io.github.oliviercailloux.keyboardd.keyboard;

import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

public interface GeometricKey {
public DoublePoint topLeftCorner();
public PositiveSize size();
}
