package io.github.oliviercailloux.keyboardd.representable;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.geometry.Zone;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeysymEntry;
import io.github.oliviercailloux.svgb.RectangleElement;
import io.github.oliviercailloux.svgb.TextElement;
import java.util.Map;
import org.w3c.dom.Document;

public class SvgRepresentedKeyboard {
  static SvgRepresentedKeyboard given(SvgKeyboard svgKeyboard, Map<TextElement, RectangleElement> textToRect, BiMap<RectangleElement, Zone> rectToZone, Map<TextElement, Zone> textToZone, Map<TextElement, CanonicalKeysymEntry> textToKeysym) {
    return new SvgRepresentedKeyboard(svgKeyboard, textToRect, rectToZone, textToZone, textToKeysym);
  }
  
  private final SvgKeyboard svgKeyboard;
  private final ImmutableMap<TextElement, RectangleElement> textToRect;
  private final ImmutableBiMap<RectangleElement, Zone> rectToZone;
  private final ImmutableMap<TextElement, Zone> textToZone;
  private final ImmutableMap<TextElement, CanonicalKeysymEntry> textToKeysym;
  
  private SvgRepresentedKeyboard(SvgKeyboard svgKeyboard,
  Map<TextElement, RectangleElement> textToRect, BiMap<RectangleElement, Zone> rectToZone,
      Map<TextElement, Zone> textToZone, Map<TextElement, CanonicalKeysymEntry> textToKeysym) {
        this.svgKeyboard = svgKeyboard;
        this.textToRect = ImmutableMap.copyOf(textToRect);
        this.rectToZone = ImmutableBiMap.copyOf(rectToZone);
        this.textToZone = ImmutableMap.copyOf(textToZone);
        this.textToKeysym = ImmutableMap.copyOf(textToKeysym);
  }

  public Document document() {
    return svgKeyboard.document();
  }

  public ImmutableMap<RectangleElement, String> keyBindingZonesToXKeyName() {
    return svgKeyboard.keyBindingZonesToXKeyName();
  }

}
