package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;

/**
 * Mnemonics to their code and possibly UCP and whether they are deprecated.
 * <ul>
 * <li>Map (non bijective, complete) from keysym mnemonic to keysym code</li>
 * <li>Map (non bijective, complete) from keysym mnemonic to whether it is deprecated</li>
 * <li>Bijective map (incomplete) from keysym code to canonical keysym mnemonic: the first listed
 * non deprecated one or the first listed if all deprecated</li>
 * <li>Map (non bijective, incomplete) from keysym mnemonic to UCP</li>
 * <li>Can also build such a bijective map without any deprecated</li>
 * <li>Can be obtained from such a bijective map (then, no deprecated)</li>
 * <li>Can be obtained from parsing a keysyms file? TODO</li>
 * <li>Can be obtained from lib? TODO</li>
 * </ul>
 */
public class Mnemonics {
  private final ImmutableMap<String, Integer> code;
  private final ImmutableMap<String, Integer> ucp;
  private final ImmutableSet<String> deprecateds;
  private final ImmutableBiMap<Integer, String> canonicalByCode;

  /**
   * 
   * @param code non bijective, complete
   * @param ucp non bijective, incomplete
   * @param deprecateds subset of the keysyms in the other maps
   * @return
   */
  public static Mnemonics from(Map<String, Integer> keysymCodeByKeysymMnemonic, Map<String, Integer> ucpByKeysymMnemonic,
      Set<String> deprecatedKeysymMnemonics) {
    return new Mnemonics(keysymCodeByKeysymMnemonic, ucpByKeysymMnemonic, deprecatedKeysymMnemonics);
  }

  private static ImmutableBiMap<Integer, String>
      canonicalsByCode(ImmutableMap<String, Integer> codeByMn, Set<String> deprecateds) {
    ImmutableSetMultimap<Integer, String> codeToMns = codeByMn.asMultimap().inverse();
    final ImmutableBiMap.Builder<Integer, String> builder = ImmutableBiMap.builder();
    for (int code : codeToMns.keySet()) {
      final ImmutableSet<String> mnemonics = codeToMns.get(code);
      final String canonical = mnemonics.stream().filter(m -> !deprecateds.contains(m)).findFirst()
          .orElse(mnemonics.iterator().next());
      builder.put(code, canonical);
    }
    ImmutableBiMap<Integer, String> canonicalsByCode = builder.build();
    verify(ImmutableSet.copyOf(codeByMn.values()).equals(canonicalsByCode.keySet()));
    return canonicalsByCode;
  }

  private Mnemonics(Map<String, Integer> keysymCodeByKeysymMnemonic,
      Map<String, Integer> ucpByKeysymMnemonic, Set<String> deprecatedKeysymMnemonics) {
    code = ImmutableMap.copyOf(keysymCodeByKeysymMnemonic);
    ucp = ImmutableMap.copyOf(ucpByKeysymMnemonic);
    deprecateds = ImmutableSet.copyOf(deprecatedKeysymMnemonics);
    checkArgument(keysymCodeByKeysymMnemonic.keySet().containsAll(deprecatedKeysymMnemonics));
    canonicalByCode = canonicalsByCode(code, deprecateds);
  }

  /**
   * 
   * @return all known keysym mnemonics.
   */
  public ImmutableSet<String> mnemonics() {
    return code.keySet();
  }

  /**
   * 
   * @return a subset of keysym mnemonics that might include deprecated ones; size equals the number of (distinct) codes.
   */
  public ImmutableSet<String> canonicals() {
    return canonicalByCode.values();
  }

  public boolean isDeprecated(String keysymMnemonic) {
    checkArgument(mnemonics().contains(keysymMnemonic));
    return deprecateds.contains(keysymMnemonic);
  }

  /**
   * 
   * @return keyset contains all mnemonics
   */
  public ImmutableMap<String, Integer> codeByMnemonic() {
    return code;
  }

  /**
   * 
   * @return keyset contains all codes, values are all canonicals, some of which might be deprecated
   */
  public ImmutableBiMap<Integer, String> canonicalByCode() {
    return canonicalByCode;
  }

  /**
   * 
   * @return keyset contains a subset of the mnemonics, some of which might be deprecated
   */
  public ImmutableMap<String, Integer> ucpByMnemonic() {
    return ucp;
  }

  /**
   * This loses codes that are mapped only to deprecated mnemonics.
   */
  public Mnemonics withoutDeprecated() {
    Map<String, Integer> filteredUcp = Maps.filterKeys(ucp, m -> !deprecateds.contains(m));
    return new Mnemonics(canonicalByCode.inverse(), filteredUcp, ImmutableSet.of());
  }
}
