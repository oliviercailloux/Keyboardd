package io.github.oliviercailloux.keyboardd.representable;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeysymEntry;
import io.github.oliviercailloux.keyboardd.mnemonics.ImplicitUcp;
import io.github.oliviercailloux.keyboardd.mnemonics.Mnemonics;
import java.util.Map;

public class DefaultCanonicalKeysymEntriesRepresentations {
  public static Map<CanonicalKeysymEntry, Representation> simpleRepresentations() {
    Mnemonics latest = Mnemonics.latest();
    
    return ImmutableMap.<CanonicalKeysymEntry, Representation>builder()
        .put(latest.canonical("BackSpace"), Representation.fromString("Backspace"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0021)), Representation.fromString("exclam"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0022)), Representation.fromString("quotedbl"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0023)), Representation.fromString("numbersign"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0024)), Representation.fromString("dollar"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0025)), Representation.fromString("percent"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0026)), Representation.fromString("ampersand"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0027)), Representation.fromString("apostrophe"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0028)), Representation.fromString("parenleft"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0029)), Representation.fromString("parenright"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002a)), Representation.fromString("asterisk"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002b)), Representation.fromString("plus"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002c)), Representation.fromString("comma"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002d)), Representation.fromString("minus"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002e)), Representation.fromString("period"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x002f)), Representation.fromString("slash"))
        .put(CanonicalKeysymEntry.from(ImplicitUcp.of(0x0030)), Representation.fromString("0"))
        .put(CanonicalKeysym
}
