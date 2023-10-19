package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

public class DisplayableKeyboardLayout {
  static DisplayableKeyboardLayout fromRows(List<? extends List<DisplayableKey>> rows) {
    return new DisplayableKeyboardLayout(rows);
  }

  /** With no empty list. Allows for duplicates. */
  private final ImmutableList<ImmutableList<DisplayableKey>> rows;

  private DisplayableKeyboardLayout(List<? extends List<DisplayableKey>> rows) {
    checkArgument(rows.stream().noneMatch(r -> r.isEmpty()));
    this.rows = rows.stream().map(r -> ImmutableList.<DisplayableKey>copyOf(r))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<ImmutableList<DisplayableKey>> rows() {
    return rows;
  }
}
