package io.github.oliviercailloux.keyboardd.visible;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Document;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.svgb.DoublePoint;

public class FunctionalKeyboard {
  /** From an SVG Physical keyboard (that can be created from a Physical Keyboard, but more general), returns a Functional SVG keyboard (same thing but with x keys transformed to represented keys) */
  public static Document from(Document svgPhysicalKeyboard, VisibleKeyboardMap visibleKeyboardMap) {
    
  }

  private FunctionalKeyboard() {}
  
}
