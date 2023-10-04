package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

public class KeyboardLayout {
  static KeyboardLayout fromRows(List<? extends List<KeyboardKey>> rows) {
    return new KeyboardLayout(rows);
  }

  /** With no empty list. Allows for duplicates (not required usually as even similar keys such as the left shift and right shift keys send different codes, it seems, but some keyboards might differ from mine in that respect). */
  private final ImmutableList<ImmutableList<KeyboardKey>> rows;

  private KeyboardLayout(List<? extends List<KeyboardKey>> rows) {
    checkArgument(rows.stream().noneMatch(r -> r.isEmpty()));
    this.rows = rows.stream().map(r -> ImmutableList.<KeyboardKey>copyOf(r))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<ImmutableList<KeyboardKey>> rows() {
    return rows;
  }
}
