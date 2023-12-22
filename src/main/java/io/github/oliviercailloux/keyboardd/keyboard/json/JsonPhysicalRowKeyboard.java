package io.github.oliviercailloux.keyboardd.keyboard.json;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKey;
import io.github.oliviercailloux.keyboardd.keyboard.PhysicalKeyboard;
import io.github.oliviercailloux.svgb.DoublePoint;
import io.github.oliviercailloux.svgb.PositiveSize;

public class JsonPhysicalRowKeyboard {
  static JsonPhysicalRowKeyboard fromRows(List<? extends List<JsonPhysicalRowKey>> rows) {
    return new JsonPhysicalRowKeyboard(rows);
  }

  /**
   * With no empty list. Allows for duplicates (not required usually as even similar keys such as
   * the left shift and right shift keys send different codes, it seems, but some keyboards might
   * differ from mine in that respect).
   */
  private final ImmutableList<ImmutableList<JsonPhysicalRowKey>> rows;

  private JsonPhysicalRowKeyboard(List<? extends List<JsonPhysicalRowKey>> rows) {
    checkArgument(rows.stream().noneMatch(r -> r.isEmpty()));
    this.rows = rows.stream().map(r -> ImmutableList.<JsonPhysicalRowKey>copyOf(r))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<ImmutableList<JsonPhysicalRowKey>> rows() {
    return rows;
  }

  public PhysicalKeyboard toPhysicalKeyboard(PositiveSize scale, PositiveSize spacing) {
    DoublePoint currentCorner = DoublePoint.zero();

    final ImmutableSet.Builder<PhysicalKey> keys = new ImmutableSet.Builder<>();
    for (ImmutableList<JsonPhysicalRowKey> row : rows) {
      for (JsonPhysicalRowKey sourceKey : row) {
        double targetWidth = sourceKey.width() * scale.x();
        PhysicalKey targetKey = PhysicalKey.from(currentCorner,
            PositiveSize.given(targetWidth, scale.y()), sourceKey.xKeyName());
        keys.add(targetKey);
        currentCorner = currentCorner.plus(PositiveSize.horizontal(targetWidth + spacing.x()));
      }
      currentCorner = DoublePoint.given(0d, scale.y() + spacing.y());
    }

    return PhysicalKeyboard.from(keys.build());
  }
}
