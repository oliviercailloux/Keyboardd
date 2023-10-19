package io.github.oliviercailloux.keyboardd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import io.github.oliviercailloux.keyboardd.draft.Key;
import io.github.oliviercailloux.keyboardd.draft.XkbReader;

public class XkbReaderTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(XkbReaderTests.class);

  @Test
  void testRead() throws Exception {
    LOGGER.info("Started tests.");
    CharSource source =
        Resources.asCharSource(XkbReaderTests.class.getResource("fr"), StandardCharsets.UTF_8);
    ImmutableList<String> lines = source.readLines();
    CharSource sourceReduced =
        CharSource.wrap(lines.subList(0, 23).stream().collect(Collectors.joining("\n")));
    ImmutableSet<Key> keys = XkbReader.read(sourceReduced);

    LOGGER.info("Keys: {}.", keys);
  }
}
