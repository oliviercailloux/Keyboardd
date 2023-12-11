package io.github.oliviercailloux.keyboardd.keyboard;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.keyboardd.Representation;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

public record FunctionalKey (DoublePoint topLeftCorner, PositiveSize size,
    ImmutableList<Representation> representations) implements GeometricKey{
  public static FunctionalKey from(DoublePoint topLeftCorner, PositiveSize size,
      List<Representation> representations) {
    return new FunctionalKey(topLeftCorner, size, ImmutableList.copyOf(representations));
  }
}
