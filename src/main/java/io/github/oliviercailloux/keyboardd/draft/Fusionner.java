package io.github.oliviercailloux.keyboardd.draft;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;

/*
 * Aggregates keysym info and builds a Mnemonic - to display table.
 * 
 * To be used later in the project!
 */
public class Fusionner {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Fusionner.class);

  public static Fusionner parse() {
    ImmutableBiMap<String, MnKeySym> keySyms = KeySymReader.parseAndPatch();
    ImmutableMap<String, Integer> mnToCode =
        Maps.toMap(keySyms.keySet(), m -> keySyms.get(m).code());
    ImmutableBiMap<Integer, String> codeToDisplay = buildDisplayTable(keySyms.values());
    ImmutableMap<String, String> mnToDisplay =
        Maps.toMap(mnToCode.keySet(), m -> codeToDisplay.get(mnToCode.get(m)));
    return new Fusionner(mnToDisplay);
  }

  private static ImmutableMap<String, String> manualMnemonicToDisplay() {
    final ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
    builder.put("Codeinput", "Hangul_Codeinput");
    builder.put("Kanji_Bangou", "Hangul_Codeinput");
    builder.put("Hangul_Codeinput", "Hangul_Codeinput");
    builder.put("SingleCandidate", "Hangul_SingleCandidate");
    builder.put("Hangul_SingleCandidate", "Hangul_SingleCandidate");
    builder.put("MultipleCandidate", "Hangul_MultipleCandidate");
    builder.put("Zen_Koho", "Hangul_MultipleCandidate");
    builder.put("Hangul_MultipleCandidate", "Hangul_MultipleCandidate");
    builder.put("PreviousCandidate", "Hangul_PreviousCandidate");
    builder.put("Mae_Koho", "Hangul_PreviousCandidate");
    builder.put("Hangul_PreviousCandidate", "Hangul_PreviousCandidate");
    builder.put("Henkan_Mode", "Henkan_Mode");
    builder.put("Henkan", "Henkan_Mode");
    builder.put("Prior", "Page_Up");
    builder.put("Page_Up", "Page_Up");
    builder.put("Next", "Page_Down");
    builder.put("Page_Down", "Page_Down");
    builder.put("Mode_switch", "Mode_switch");
    builder.put("script_switch", "Mode_switch");
    builder.put("ISO_Group_Shift", "Mode_switch");
    builder.put("kana_switch", "Mode_switch");
    builder.put("Arabic_switch", "Mode_switch");
    builder.put("Greek_switch", "Mode_switch");
    builder.put("Hebrew_switch", "Mode_switch");
    builder.put("Hangul_switch", "Mode_switch");
    builder.put("KP_Prior", "KP_Page_Up");
    builder.put("KP_Page_Up", "KP_Page_Up");
    builder.put("KP_Next", "KP_Page_Down");
    builder.put("KP_Page_Down", "KP_Page_Down");
    builder.put("F11", "F11");
    builder.put("L1", "F11");
    builder.put("F12", "F12");
    builder.put("L2", "F12");
    builder.put("F13", "F13");
    builder.put("L3", "F13");
    builder.put("F14", "F14");
    builder.put("L4", "F14");
    builder.put("F15", "F15");
    builder.put("L5", "F15");
    builder.put("F16", "F16");
    builder.put("L6", "F16");
    builder.put("F17", "F17");
    builder.put("L7", "F17");
    builder.put("F18", "F18");
    builder.put("L8", "F18");
    builder.put("F19", "F19");
    builder.put("L9", "F19");
    builder.put("F20", "F20");
    builder.put("L10", "F20");
    builder.put("F21", "F21");
    builder.put("R1", "F21");
    builder.put("F22", "F22");
    builder.put("R2", "F22");
    builder.put("F23", "F23");
    builder.put("R3", "F23");
    builder.put("F24", "F24");
    builder.put("R4", "F24");
    builder.put("F25", "F25");
    builder.put("R5", "F25");
    builder.put("F26", "F26");
    builder.put("R6", "F26");
    builder.put("F27", "F27");
    builder.put("R7", "F27");
    builder.put("F28", "F28");
    builder.put("R8", "F28");
    builder.put("F29", "F29");
    builder.put("R9", "F29");
    builder.put("F30", "F30");
    builder.put("R10", "F30");
    builder.put("F31", "F31");
    builder.put("R11", "F31");
    builder.put("F32", "F32");
    builder.put("R12", "F32");
    builder.put("F33", "F33");
    builder.put("R13", "F33");
    builder.put("F34", "F34");
    builder.put("R14", "F34");
    builder.put("F35", "F35");
    builder.put("R15", "F35");
    /*
     * Also #define XK_combining_tilde 0x1000303 /* U+0303 COMBINING TILDE thus can’t use that
     * character.
     */
    builder.put("dead_tilde", "Dead \u0303");
    builder.put("dead_perispomeni", "Dead \u0303");
    builder.put("dead_abovecomma", "Dead \u0313");
    builder.put("dead_psili", "Dead \u0313");
    builder.put("dead_abovereversedcomma", "Dead \u0314");
    builder.put("dead_dasia", "Dead \u0314");
    return builder.build();
  }

  private static ImmutableBiMap<Integer, String> buildDisplayTable(Set<MnKeySym> syms) {
    ImmutableMap<String, String> manualMnemonicToDisplay = manualMnemonicToDisplay();

    // final ImmutableBiMap.Builder<Integer, String> tableBuilder = new ImmutableBiMap.Builder<>();
    HashBiMap<Integer, String> tableBuilder = HashBiMap.create();
    // final ImmutableSetMultimap.Builder<Integer, String> tableBuilder =
    // new ImmutableSetMultimap.Builder<>();
    for (MnKeySym sym : syms) {
      Optional<String> manual = Optional.ofNullable(manualMnemonicToDisplay.get(sym.mnemonic()));
      String display = sym.unicodeAsString().orElse(manual.orElse(sym.mnemonic()));
      if (tableBuilder.containsKey(sym.code()))
        verify(tableBuilder.get(sym.code()).equals(display));
      else if (tableBuilder.containsValue(display))
        LOGGER.warn("Dupl codes {} and {} for {}.", tableBuilder.inverse().get(display), sym.code(),
            display);
      else
        tableBuilder.put(sym.code(), display);
    }
    // tableBuilder.build().asMap().values().stream().filter(s -> s.size() >= 2)
    // .forEach(s -> LOGGER.warn("Dupl for some code: {}.", s));
    // return null;
    return ImmutableBiMap.copyOf(tableBuilder);
  }

  private ImmutableMap<String, String> mnToDisplay;

  private Fusionner(Map<String, String> mnToDisplay) {
    this.mnToDisplay = ImmutableMap.copyOf(mnToDisplay);
  }
}
