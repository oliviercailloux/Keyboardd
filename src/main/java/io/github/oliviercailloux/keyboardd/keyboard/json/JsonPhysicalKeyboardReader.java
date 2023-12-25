package io.github.oliviercailloux.keyboardd.keyboard.json;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharSource;

public class JsonPhysicalKeyboardReader {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonPhysicalKeyboardReader.class);

  private JsonPhysicalKeyboardReader() {
  }

  public static JsonPhysicalRowKeyboard rowKeyboard(CharSource source) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JavaType listOfKeysType =
        mapper.getTypeFactory().constructCollectionType(List.class, JsonPhysicalRowKey.class);
    JavaType listOfListOfKeysType = mapper.getTypeFactory().constructCollectionType(List.class, listOfKeysType);
    try (Reader reader = source.openStream()) {
      List<List<JsonPhysicalRowKey>> rows = mapper.readValue(reader, listOfListOfKeysType);
      return JsonPhysicalRowKeyboard.fromRows(rows);
    }
  }
}
