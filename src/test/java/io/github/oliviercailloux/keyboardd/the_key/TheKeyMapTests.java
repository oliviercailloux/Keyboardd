package io.github.oliviercailloux.keyboardd.the_key;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.mapping.KeyboardMap;
import io.github.oliviercailloux.keyboardd.mapping.KeysymEntry.Mnemonic;
import io.github.oliviercailloux.keyboardd.mapping.XkbSymbolsReader;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeyboardMap;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalKeysymEntry;
import io.github.oliviercailloux.keyboardd.mnemonics.CanonicalMnemonic;
import io.github.oliviercailloux.keyboardd.mnemonics.Mnemonics;
import io.github.oliviercailloux.keyboardd.representable.Representation;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import io.github.oliviercailloux.keyboardd.representable.XKeyNamesAndRepresenter;
import io.github.oliviercailloux.keyboardd.xkeys.Xkeys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class TheKeyMapTests {

  @Test
  public void writeMappedDefault() throws IOException {
    Document inputDocument =
        DomHelper.domHelper().asDocument(PathUtils.fromResource(getClass(), "The Key.svg"));
    SvgKeyboard inputSvg = SvgKeyboard.using(inputDocument);

    KeyboardMap map = XkbSymbolsReader.common().overwrite(XkbSymbolsReader.us());
    CanonicalKeyboardMap canonMap = CanonicalKeyboardMap
        .canonicalize(map.canonicalize(Xkeys.latest().canonicalByAlias()), Mnemonics.latest());
    XKeyNamesAndRepresenter representer =
        XKeyNamesAndRepresenter.from(canonMap, XKeyNamesAndRepresenter::defaultRepresentation);
    Document outputDocument = inputSvg.withRepresentations(representer::representations);
    String outputString = DomHelper.domHelper().toString(outputDocument);
    Files.writeString(Path.of("out.svg"), outputString);

    CharSource expectedOutput = Resources.asCharSource(
        getClass().getResource("The Key with default common+us representations.svg"), StandardCharsets.UTF_8);
    assertEquals(expectedOutput.read(), outputString);
  }

  @Test
  public void writeMappedTweaked() throws IOException {
    Document inputDocument =
        DomHelper.domHelper().asDocument(PathUtils.fromResource(getClass(), "The Key.svg"));
    SvgKeyboard inputSvg = SvgKeyboard.using(inputDocument);

    KeyboardMap map = XkbSymbolsReader.common().overwrite(XkbSymbolsReader.us());
    CanonicalKeyboardMap canonMap = CanonicalKeyboardMap
        .canonicalize(map.canonicalize(Xkeys.latest().canonicalByAlias()), Mnemonics.latest());
    XKeyNamesAndRepresenter representer = XKeyNamesAndRepresenter.from(canonMap, this::represent);
    Document outputDocument = inputSvg.withRepresentations(representer::representations);
    String outputString = DomHelper.domHelper().toString(outputDocument);
    Files.writeString(Path.of("out.svg"), outputString);

    CharSource expectedOutput = Resources.asCharSource(
        getClass().getResource("The Key with tweaked common+us representations.svg"), StandardCharsets.UTF_8);
    assertEquals(expectedOutput.read(), outputString);
  }

  private Representation represent(CanonicalKeysymEntry entry) {
    if (entry instanceof CanonicalMnemonic mnemonic) {
      if (mnemonic.mnemonic().equals("Control_L")) {
        return Representation.fromString("Ctrl");
      }
    }
    return XKeyNamesAndRepresenter.defaultRepresentation(entry);
  }
}
