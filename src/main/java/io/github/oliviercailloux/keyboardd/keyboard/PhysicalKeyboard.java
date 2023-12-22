package io.github.oliviercailloux.keyboardd.keyboard;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.svgb.DoublePoint;

/** With rectangle keys. The SvgPK is a more general concept. 
 * The unit is 1 cm.
*/
public class PhysicalKeyboard {
  public static PhysicalKeyboard from(Set<PhysicalKey> physicalKeys) {
    return new PhysicalKeyboard(physicalKeys);
  }

  /**
   * Allows for duplicate x key names (not required usually as even similar keys such as
   * the left shift and right shift keys send different codes, it seems, but some keyboards might
   * differ from mine in that respect).
   */
  private final ImmutableSet<PhysicalKey> keys;

  private PhysicalKeyboard(Set<PhysicalKey> physicalKeys) {
    this.keys = ImmutableSet.copyOf(physicalKeys);
    ImmutableMultiset<DoublePoint> corners =
        physicalKeys.stream().map(k -> k.topLeftCorner()).collect(ImmutableMultiset.toImmutableMultiset());
    checkArgument(corners.size() == corners.entrySet().size());
    if (!physicalKeys.isEmpty())
      checkArgument(corners.stream().anyMatch(c -> c.equals(DoublePoint.zero())));
  }

  public ImmutableSet<PhysicalKey> keys() {
    return keys;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof PhysicalKeyboard)) {
      return false;
    }
    final PhysicalKeyboard t2 = (PhysicalKeyboard) o2;
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
