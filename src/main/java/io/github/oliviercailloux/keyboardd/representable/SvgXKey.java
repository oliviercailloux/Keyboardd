package io.github.oliviercailloux.keyboardd.representable;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSortedSet;
import io.github.oliviercailloux.geometry.Point;
import io.github.oliviercailloux.geometry.Zone;
import io.github.oliviercailloux.svgb.RectangleElement;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/** Not really immutable as the XML elements are not. */
public class SvgXKey {
  static SvgXKey create(String xKeyName, Zone keyZone, RectangleElement rectangle) {
    return new SvgXKey(xKeyName, keyZone, rectangle);
  }

  private final String xKeyName;
  private final Zone keyZone;
  private final RectangleElement rectangle;
  private ImmutableSortedSet<SvgKeysymEntry> svgKeysymEntries;

  private SvgXKey(String xKeyName, Zone keyZone, RectangleElement rectangle) {
    this.xKeyName = xKeyName;
    this.keyZone = keyZone;
    this.rectangle = rectangle;
    this.svgKeysymEntries = null;
  }

  void setContent(Set<SvgKeysymEntry> entries) {
    checkArgument(entries.stream().map(SvgKeysymEntry::zone).distinct().count() == entries.size());
    Comparator<Point> pointComparator = Comparator.comparing(Point::x).thenComparing(Point::y,
        Comparator.<Double>naturalOrder().reversed());
    Comparator<Zone> zoneComparator = Comparator.comparing(Zone::start, pointComparator)
        .thenComparing(Zone::end, pointComparator);
    this.svgKeysymEntries = ImmutableSortedSet
        .copyOf(Comparator.comparing(SvgKeysymEntry::zone, zoneComparator), entries);
  }

  public String xKeyName() {
    return xKeyName;
  }

  public Zone keyZone() {
    return keyZone;
  }

  public RectangleElement rectangle() {
    return rectangle;
  }

  public ImmutableSortedSet<SvgKeysymEntry> svgKeysymEntries() {
    return svgKeysymEntries;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof SvgXKey)) {
      return false;
    }
    final SvgXKey t2 = (SvgXKey) o2;
    return xKeyName.equals(t2.xKeyName) && keyZone.equals(t2.keyZone)
        && rectangle.equals(t2.rectangle) && svgKeysymEntries.equals(t2.svgKeysymEntries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(xKeyName, keyZone, rectangle, svgKeysymEntries);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("xKeyName", xKeyName).add("keyZone", keyZone)
        .add("rectangle", rectangle).add("svgKeysymEntries", svgKeysymEntries).toString();
  }
}
