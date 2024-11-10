package io.github.oliviercailloux.keyboardd.representable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Function;

public interface XKeyNamesRepresenter {
  public static XKeyNamesRepresenter using(Function<String, List<Representation>> representations) {
    return XKeyNamesRepresenterImpl.using(representations);
  }

  ImmutableList<Representation> representations(String name);
}
