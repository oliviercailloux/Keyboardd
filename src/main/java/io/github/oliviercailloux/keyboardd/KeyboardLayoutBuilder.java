package io.github.oliviercailloux.keyboardd;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;

public class KeyboardLayoutBuilder {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(KeyboardLayoutBuilder.class);

  public static KeyboardLayoutBuilder parse() {
    ImmutableBiMap<String, Integer> namesToCodes = EvdevReader.parse();
    return new KeyboardLayoutBuilder(namesToCodes);
  }

  private ImmutableBiMap<String, Integer> namesToCodes;

  private KeyboardLayoutBuilder(BiMap<String, Integer> namesToCodes) {
    this.namesToCodes = ImmutableBiMap.copyOf(namesToCodes);
  }

  public KeyboardLayout getLayout(CharSource source) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, KeyboardKey.class);
    JavaType type2 = mapper.getTypeFactory().constructCollectionType(List.class, type);
    try (Reader reader = source.openStream()) {
      List<List<KeyboardKey>> rows = mapper.readValue(reader, type2);
      {
        ImmutableList<KeyboardKey> keys =
            rows.stream().flatMap(r -> r.stream()).collect(ImmutableList.toImmutableList());
        ImmutableSet<String> names =
            keys.stream().map(KeyboardKey::name).collect(ImmutableSet.toImmutableSet());
        // ImmutableMultiset<KeyboardKey> keysMulti = ImmutableMultiset.copyOf(keys);
        // ImmutableSet<KeyboardKey> keysDupl = keysMulti.elementSet().stream()
        //     .filter(s -> keysMulti.count(s) >= 2).collect(ImmutableSet.toImmutableSet());
        // KeyboardKey firstDupl = keysDupl.iterator().next();
        // LOGGER.info("First dupl {}, count {}.", firstDupl, keysMulti.count(firstDupl));
        // checkArgument(keys.size() == names.size(), keysDupl);
        final ImmutableSet<String> knownNames = new ImmutableSet.Builder<String>().addAll(namesToCodes.keySet()).add("").build();
        
        ImmutableSet<String> namesMissing =
            Sets.difference(names, knownNames).immutableCopy();
        // String miss = namesMissing.iterator().next();
        // verify(miss.isEmpty());
        verify(namesMissing.isEmpty(), namesMissing.toString());
      }
      return KeyboardLayout.fromRows(rows);
    }
  }
}
