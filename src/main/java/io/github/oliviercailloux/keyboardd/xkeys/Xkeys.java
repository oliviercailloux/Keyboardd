package io.github.oliviercailloux.keyboardd.xkeys;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.util.Map;

/**
 * The set of X key names (canonical and aliases) and corresponding X keycodes available on a
 * system.
 */
public interface Xkeys {
  public static Xkeys latest() {
    return EvdevReader.latest();
  }

  /**
   *
   * @param xKeycodes
   * @param canonicalXKeyNameByAlias keys are all aliases, values are only canonicals
   * @return
   */
  public static Xkeys fromMaps(Map<String, Short> xKeycodes,
      Map<String, String> canonicalXKeyNameByAlias) {
    return XkeysImpl.fromMaps(xKeycodes, canonicalXKeyNameByAlias);
  }

  public static Xkeys fromSource(CharSource source) throws IOException {
    return EvdevReader.parse(source);
  }

  ImmutableSet<Short> codes();

  /**
   *
   * @return disjoint from aliases
   */
  ImmutableSet<String> canonicals();

  String canonical(short code);

  /**
   *
   * @param keyName a legal X key name
   * @return equal to argument iff given is canonical
   */
  String canonical(String keyName);

  /**
   *
   * @return key set equals the canonicals; values equals the legal codes
   */
  ImmutableBiMap<String, Short> codeByCanonical();

  /**
   * @return key set equals the legal codes; values equals the canonicals
   */
  ImmutableBiMap<Short, String> canonicalByCode();

  /**
   *
   * @return disjoint from canonicals
   */
  ImmutableSet<String> aliases();

  /**
   *
   * @param canonicalXKeyName
   * @return may be empty, not containing the canonical
   */
  ImmutableSet<String> aliases(String canonicalXKeyName);

  /**
   *
   * @return key set equals the aliases; values include only canonicals
   */
  ImmutableMap<String, String> canonicalByAlias();

  /**
   *
   * @return union of canonicals and aliases
   */
  ImmutableSet<String> names();

  /**
   *
   * @param code a legal code
   * @return not empty, canonical is first element
   */
  ImmutableSet<String> names(short code);

  /**
   *
   * @param keyName a legal X key name
   * @return true iff is not canonical
   */
  boolean isAlias(String keyName);

  /**
   *
   * @param keyName a legal X key name
   * @return
   */
  short code(String keyName);

  /**
   *
   * @return key set equals the legal names; values equals the legal codes;
   */
  ImmutableMap<String, Short> codeByName();

  Xkeys withoutAliases();

  @Override
  boolean equals(Object o2);
}
