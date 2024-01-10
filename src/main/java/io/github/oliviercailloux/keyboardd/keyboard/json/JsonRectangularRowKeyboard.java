package io.github.oliviercailloux.keyboardd.keyboard.json;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.keyboardd.keyboard.RectangularKey;
import io.github.oliviercailloux.keyboardd.keyboard.RectangularKeyboard;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

public class JsonRectangularRowKeyboard {
  static JsonRectangularRowKeyboard fromRows(List<? extends List<JsonRectangularRowKey>> rows) {
    return new JsonRectangularRowKeyboard(rows);
  }

  /**
   * With no empty list. Allows for duplicates (not required usually as even similar keys such as
   * the left shift and right shift keys send different codes, it seems, but some keyboards might
   * differ from mine in that respect).
   */
  private final ImmutableList<ImmutableList<JsonRectangularRowKey>> rows;

  private JsonRectangularRowKeyboard(List<? extends List<JsonRectangularRowKey>> rows) {
    checkArgument(rows.stream().noneMatch(r -> r.isEmpty()));
    this.rows = rows.stream().map(r -> ImmutableList.<JsonRectangularRowKey>copyOf(r))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<ImmutableList<JsonRectangularRowKey>> rows() {
    return rows;
  }

  public RectangularKeyboard toPhysicalKeyboard(PositiveSize scale, PositiveSize spacing) {
    DoublePoint currentCorner = DoublePoint.zero();

    final ImmutableSet.Builder<RectangularKey> keys = new ImmutableSet.Builder<>();
    for (ImmutableList<JsonRectangularRowKey> row : rows) {
      for (JsonRectangularRowKey sourceKey : row) {
        double targetWidth = sourceKey.width() * scale.x();
        RectangularKey targetKey = RectangularKey.from(currentCorner,
            PositiveSize.given(targetWidth, scale.y()), sourceKey.xKeyName());
        keys.add(targetKey);
        currentCorner = currentCorner.plus(PositiveSize.horizontal(targetWidth + spacing.x()));
      }
      currentCorner = DoublePoint.given(0d, scale.y() + spacing.y());
    }

    return RectangularKeyboard.from(keys.build());
  }
}
