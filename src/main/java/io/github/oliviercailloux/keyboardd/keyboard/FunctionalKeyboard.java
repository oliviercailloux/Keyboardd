package io.github.oliviercailloux.keyboardd.keyboard;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.svgb.DoublePoint;

public class FunctionalKeyboard {
  public static FunctionalKeyboard from(Set<FunctionalKey> functionalKeys) {
    return new FunctionalKeyboard(functionalKeys);
  }

  public static FunctionalKeyboard from(PhysicalKeyboard physicalKeyboard, VisibleKeyboardMap visibleKeyboardMap) {
    ImmutableSet.Builder<FunctionalKey> builder = ImmutableSet.builder();
    for (PhysicalKey physicalKey : physicalKeyboard.keys()) {
      builder.add(FunctionalKey.from(physicalKey.topLeftCorner(), physicalKey.size(), visibleKeyboardMap.representations(physicalKey.xKeyName())));
    }
    return new FunctionalKeyboard(builder.build());
  }
  
  private final ImmutableSet<FunctionalKey> keys;

  private FunctionalKeyboard(Set<FunctionalKey> functionalKeys) {
    this.keys = ImmutableSet.copyOf(functionalKeys);
    ImmutableMultiset<DoublePoint> corners =
        functionalKeys.stream().map(k -> k.topLeftCorner()).collect(ImmutableMultiset.toImmutableMultiset());
    checkArgument(corners.size() == corners.entrySet().size());
    if (!functionalKeys.isEmpty())
      checkArgument(corners.stream().anyMatch(c -> c.equals(DoublePoint.zero())));
  }

  public ImmutableSet<FunctionalKey> keys() {
    return keys;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof FunctionalKeyboard)) {
      return false;
    }
    final FunctionalKeyboard t2 = (FunctionalKeyboard) o2;
    return keys.equals(t2.keys);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(keys);
  }
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("functionalKeys", keys).toString();
  }
  
}
