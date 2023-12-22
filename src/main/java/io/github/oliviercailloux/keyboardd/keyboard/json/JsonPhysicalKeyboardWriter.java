package io.github.oliviercailloux.keyboardd.keyboard.json;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.CharSink;

public class JsonPhysicalKeyboardWriter {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonPhysicalKeyboardWriter.class);
  
  public static void write(JsonPhysicalRowKeyboard keyboard, CharSink output) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter objectWriter = mapper.writerWithDefaultPrettyPrinter();
    try (Writer writer = output.openBufferedStream()) {
      objectWriter.writeValue(writer, keyboard.rows());
    }
  }
  
  public static String toJsonString(JsonPhysicalRowKeyboard keyboard) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
Indenter indenter = new DefaultIndenter();
printer.indentObjectsWith(indenter); // Indent JSON objects
printer.indentArraysWith(indenter);  // Indent JSON arrays
    ObjectWriter objectWriter = mapper.writer(printer);
    return objectWriter.writeValueAsString(keyboard.rows());
  }
}
