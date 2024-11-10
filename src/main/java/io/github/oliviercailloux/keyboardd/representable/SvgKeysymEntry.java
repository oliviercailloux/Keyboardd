package io.github.oliviercailloux.keyboardd.representable;

import io.github.oliviercailloux.geometry.Zone;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeysymEntry;
import io.github.oliviercailloux.svgb.TextElement;

public record SvgKeysymEntry(CanonicalKeysymEntry canonicalKeysymEntry, Zone zone, TextElement textElement, SvgXKey xKey) {

}