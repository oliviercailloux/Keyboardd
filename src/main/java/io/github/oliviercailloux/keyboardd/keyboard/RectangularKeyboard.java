package io.github.oliviercailloux.keyboardd.keyboard;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.svgb.DoublePoint;

/**
 * A keyboard, conceived as a set of keys that each have a position, a rectangular shape, a size,
 * and an X key name corresponding to the signal that it sends.
 * <p>
 * The unit is 1 cm.
 * <p>
 * Two such keyboards are equal iff they have equal sets of keys.
 */
public class RectangularKeyboard {
  /**
   * Builds a rectangular keyboard from a set of rectangular keys.
   * <p>
   * If the given set of keys overlaps (i.e., if two keys have a common point), then the behavior is
   * unspecified. Future versions of this library may throw an exception in that case.
   * 
   * @param physicalKeys may be empty; one key must have the top left corner at the origin
   *        ({@link DoublePoint#zero()}); duplicate X key names are allowed
   * @return a rectangular keyboard
   */
  public static RectangularKeyboard from(Set<RectangularKey> physicalKeys) {
    return new RectangularKeyboard(physicalKeys);
  }

  /**
   * Allows for duplicate x key names (not required usually, it seems, as even similar keys such as
   * the left shift and right shift keys send different codes, but some keyboards might differ from
   * mine in that respect).
   */
  private final ImmutableSet<RectangularKey> keys;

  private RectangularKeyboard(Set<RectangularKey> physicalKeys) {
    this.keys = ImmutableSet.copyOf(physicalKeys);
    ImmutableMultiset<DoublePoint> corners = physicalKeys.stream().map(k -> k.topLeftCorner())
        .collect(ImmutableMultiset.toImmutableMultiset());
    checkArgument(corners.size() == corners.entrySet().size());
    if (!physicalKeys.isEmpty())
      checkArgument(corners.stream().anyMatch(c -> c.equals(DoublePoint.zero())));
  }

  /**
   * Returns the set of keys that compose this keyboard.
   * 
   * @return a possibly empty set of keys, one of which having the top left corner at the origin
   *         ({@link DoublePoint#zero()})
   */
  public ImmutableSet<RectangularKey> keys() {
    return keys;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof RectangularKeyboard)) {
      return false;
    }
    final RectangularKeyboard t2 = (RectangularKeyboard) o2;
    return keys.equals(t2.keys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keys);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("physicalKeys", keys).toString();
  }
}
