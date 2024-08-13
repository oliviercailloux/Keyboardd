package io.github.oliviercailloux.keyboardd.the_key;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.keyboardd.representable.Representation;
import io.github.oliviercailloux.keyboardd.representable.SvgKeyboard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class TheKeyXKeyNamesTests {
  @Test
  public void writeWithXKeyNames() throws IOException {
    Document inputDocument =
        DomHelper.domHelper().asDocument(PathUtils.fromResource(getClass(), "The Key.svg"));
    SvgKeyboard inputSvg = SvgKeyboard.using(inputDocument);
    Document outputDocument = inputSvg.withRepresentations(this::representName);
    String outputString = DomHelper.domHelper().toString(outputDocument);

    CharSource expectedOutput = Resources.asCharSource(
        getClass().getResource("The Key with X key names.svg"), StandardCharsets.UTF_8);
    assertEquals(expectedOutput.read(), outputString);
  }

  private ImmutableList<Representation> representName(String xKeyName) {
    return ImmutableList.of(Representation.fromString(xKeyName));
  }

  @Test
  public void writeWithChosenRepresentations() throws IOException {
    Document inputDocument =
        DomHelper.domHelper().asDocument(PathUtils.fromResource(getClass(), "The Key.svg"));
    SvgKeyboard inputSvg = SvgKeyboard.using(inputDocument);
    inputSvg.setFontSize(15);
    Document outputDocument = inputSvg.withRepresentations(this::represent);
    String outputString = DomHelper.domHelper().toString(outputDocument);
    
    CharSource expectedOutput = Resources.asCharSource(
        getClass().getResource("The Key with chosen representations.svg"), StandardCharsets.UTF_8);
    assertEquals(expectedOutput.read(), outputString);
  }

  private ImmutableList<Representation> represent(String xKeyName) {
    final String repr = switch(xKeyName) {
      case "LCTL" -> "Ctrl";
      case "AB03" -> "C";
      case "AB04" -> "V";
      default -> xKeyName;
    };
    return ImmutableList.of(Representation.fromString(repr));
  }
}
