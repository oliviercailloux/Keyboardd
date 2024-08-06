package io.github.oliviercailloux.keyboardd.representable;

import java.util.List;
import java.util.function.Function;

class XKeyNamesRepresenterImpl implements XKeyNamesRepresenter {
  public static XKeyNamesRepresenterImpl using(Function<String, List<Representation>> representations) {
    return new XKeyNamesRepresenterImpl(representations);
  }

  private final Function<String, List<Representation>> representations;

  private XKeyNamesRepresenterImpl(Function<String, List<Representation>> representations) {
    this.representations = representations;
  }

  public List<Representation> representations(String name) {
    return representations.apply(name);
  }
}
