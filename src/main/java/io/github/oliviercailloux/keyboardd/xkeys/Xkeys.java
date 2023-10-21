package io.github.oliviercailloux.keyboardd.xkeys;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;

/**
 * <ul>
 * <li>Map (non bijective, complete) from X key name to X keycode</li>
 * <li>Map (non bijective, complete) from new X key name to canonical X key name</li>
 * <li>Bijective map (complete) from X keycode to canonical X key name (the one that is not an
 * alias).</li>
 * <li>Can be obtained from the two maps from X key name here above</li>
 * <li>Can be obtained from the maps from X keycode here above (then has no aliases)</li>
 * <li>Can be obtained from parsing evdev</li>
 * <li>Provide “without aliases” method which returns an X keys without aliases</li>
 * <li>TODO Can be obtained from lib?</li>
 * </ul>
 */
public interface Xkeys {
  public static Xkeys latest() {
    return EvdevReader.latest();
  }

  /**
   * 
   * @param codes
   * @param canonicalByAlias keys are all aliases, values are only canonicals
   * @return
   */
  public static Xkeys fromMaps(Map<String, Short> codes, Map<String, String> canonicalByAlias) {
    return XkeysImpl.fromMaps(codes, canonicalByAlias);
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
   * @param canonical
   * @return may be empty, not containing the canonical
   */
  ImmutableSet<String> aliases(String canonical);

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
   * @param keyName a legal X key name
   * @return true iff is not canonical
   */
  boolean isAlias(String keyName);

  /**
   * 
   * @param keyName a legal X key name
   * @return equal to argument iff given is canonical
   */
  String canonical(String keyName);

  /**
   * 
   * @param keyName a legal X key name
   * @return
   */
  short code(String keyName);

  /**
   * 
   * @param code a legal code
   * @return not empty, canonical is first element
   */
  ImmutableSet<String> names(short code);

  /**
   * 
   * @return key set equals the legal names; values equals the legal codes;
   */
  ImmutableMap<String, Short> codeByName();
}
