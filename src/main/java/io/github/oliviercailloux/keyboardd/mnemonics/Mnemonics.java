package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.github.oliviercailloux.keyboardd.mnemonics.KeySymReader.ParsedMnemonic;

/**
 * Mnemonics to their code and possibly UCP and whether they are deprecated.
 * <ul>
 * <li>Map (non bijective, complete) from keysym mnemonic to keysym code</li>
 * <li>Map (non bijective, complete) from keysym mnemonic to whether it is deprecated</li>
 * <li>Bijective map (incomplete) from keysym code to canonical keysym mnemonic: a non deprecated
 * one if possible (no guarantee that it’s the first listed, as lambda is an alias to lamda</li>
 * <li>Map (non bijective, incomplete) from keysym mnemonic to UCP</li>
 * <li>Can also build such a bijective map without any deprecated</li>
 * <li>Can be obtained from such a bijective map (then, no deprecated)</li>
 * <li>Can be obtained from parsing a keysyms file?</li>
 * <li>Can be obtained from lib?</li>
 * </ul>
 */
public class Mnemonics {
  public static record CanonicalMnemonic (String mnemonic, int code, ImmutableSet<String> nonDeprecatedAliases,
      ImmutableSet<String> deprecatedAliases, Optional<Integer> ucp, boolean deprecated) {
    public CanonicalMnemonic {
      checkArgument(!mnemonic.isEmpty());
      checkArgument(Sets.intersection(nonDeprecatedAliases, deprecatedAliases).isEmpty());
      if (deprecated) {
        checkArgument(nonDeprecatedAliases.isEmpty());
      }
    }

    public ImmutableSet<String> mnemonics() {
      return ImmutableSet.<String>builder().add(mnemonic)
          .addAll(nonDeprecatedAliases).addAll(deprecatedAliases).build();
    }

    public ImmutableSet<String> aliases() {
      return ImmutableSet.<String>builder().addAll(nonDeprecatedAliases).addAll(deprecatedAliases)
          .build();
    }
  }

  private final ImmutableMap<String, Integer> code;
  private final ImmutableMap<String, Integer> ucp;
  private final ImmutableBiMap<String, CanonicalMnemonic> canonicals;
  private final ImmutableMap<String, CanonicalMnemonic> canonicalsByAlias;

  public static Mnemonics latest() {
    ImmutableSet<ParsedMnemonic> parsedMns = KeySymReader.latest();
    ImmutableSet<ParsedMnemonic> patchedMns = KeySymReader.patch(parsedMns);
    ImmutableBiMap<String, ParsedMnemonic> canonicalMns = patchedMns.stream().filter(m -> !m.alias()).collect(ImmutableBiMap.toImmutableBiMap(ParsedMnemonic::mnemonic, m -> m));
    ImmutableSetMultimap<String, ParsedMnemonic> aliasesByCanonical = patchedMns.stream().filter(m -> m.alias()).collect(ImmutableSetMultimap.toImmutableSetMultimap(ParsedMnemonic::remainingComment, m -> m));
    //ImmutableSetMultimap<ParsedMnemonic, ParsedMnemonic> aliasesByCanonical = patchedMns.stream().filter(m -> m.alias()).collect(ImmutableSetMultimap.toImmutableSetMultimap(m -> canonicalMns.get(m.remainingComment()), m -> m));
    ImmutableSet.Builder<CanonicalMnemonic> canonicalMnemonicsBuilder = ImmutableSet.builder();
    for (ParsedMnemonic mn : canonicalMns.values()) {
      String canonical = mn.mnemonic();
      ImmutableSet<ParsedMnemonic> allAliases = aliasesByCanonical.get(canonical);
      ImmutableSet<String> nonDeprecatedaliases = allAliases.stream().filter(m -> !m.deprecated()).map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet());
      ImmutableSet<String> deprecatedAliases = allAliases.stream().filter(ParsedMnemonic::deprecated).map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet());
      CanonicalMnemonic canonicalMnemonic = new CanonicalMnemonic(canonical, mn.code(), nonDeprecatedaliases, deprecatedAliases, mn.unicode(), mn.deprecated());
      canonicalMnemonicsBuilder.add(canonicalMnemonic);
    }
    return new Mnemonics(canonicalMnemonicsBuilder.build());
  }

  public static Mnemonics from(Set<CanonicalMnemonic> canonicalMnemonics) {
    return new Mnemonics(canonicalMnemonics);
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

  private Mnemonics(Set<CanonicalMnemonic> canonicalKeysymMnemonics) {
    canonicals = canonicalKeysymMnemonics.stream()
        .collect(ImmutableBiMap.toImmutableBiMap(CanonicalMnemonic::mnemonic, c -> c));

    final ImmutableMap.Builder<String, Integer> keysymCodeByKeysymMnemonicBuilder =
        ImmutableMap.builder();
    for (CanonicalMnemonic canonical : canonicalKeysymMnemonics) {
      int curCode = canonical.code();
      for (String mnemonic : canonical.mnemonics()) {
        keysymCodeByKeysymMnemonicBuilder.put(mnemonic, curCode);
      }
    }
    code = keysymCodeByKeysymMnemonicBuilder.build();

    final ImmutableMap.Builder<String, Integer> ucpByKeysymMnemonicBuilder =
        ImmutableMap.builder();
    for (CanonicalMnemonic canonical : canonicalKeysymMnemonics) {
      Optional<Integer> optUcp = canonical.ucp();
      if (optUcp.isEmpty()) {
        continue;
      }
      int curUcp = optUcp.orElseThrow();
      for (String mnemonic : canonical.mnemonics()) {
        ucpByKeysymMnemonicBuilder.put(mnemonic, curUcp);
      }
    }

    ucp = ucpByKeysymMnemonicBuilder.build();

    final ImmutableMap.Builder<String, CanonicalMnemonic> canonicalsByAliasBuilder =
        ImmutableMap.builder();
    for (CanonicalMnemonic canonical : canonicalKeysymMnemonics) {
      for (String alias : canonical.aliases()) {
        canonicalsByAliasBuilder.put(alias, canonical);
      }
    }
    canonicalsByAlias = canonicalsByAliasBuilder.build();

    /* Checks that each code has zero or one ucp. */
    ucpByCode();
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
   * @return a subset of keysym mnemonics that might include deprecated ones.
   */
  public ImmutableSet<String> canonicals() {
    return canonicals.keySet();
  }

  public boolean isDeprecated(String keysymMnemonic) {
    checkArgument(mnemonics().contains(keysymMnemonic));
    boolean isCanonical = canonicals.containsKey(keysymMnemonic);
    boolean isAlias = canonicalsByAlias.containsKey(keysymMnemonic);
    verify(isCanonical != isAlias);

    if(isCanonical) {
      CanonicalMnemonic canonical;
      canonical= canonicals.get(keysymMnemonic);
      return canonical.deprecated();
    } 
    CanonicalMnemonic canonical;
      canonical= canonicalsByAlias.get(keysymMnemonic);
    verify(canonical.aliases().contains(keysymMnemonic));
    boolean isNonDepr = canonical.nonDeprecatedAliases().contains(keysymMnemonic);
    boolean isDepr = canonical.deprecatedAliases().contains(keysymMnemonic);
    verify(isNonDepr != isDepr);
    return isDepr;
  }

  public ImmutableSet<String> aliases(String canonicalKeysymMnemonic) {
    checkArgument(canonicals.containsKey(canonicalKeysymMnemonic));
    return canonicals.get(canonicalKeysymMnemonic).aliases();
  }
  
  public ImmutableSet<String> nonDeprecatedAliases(String canonicalKeysymMnemonic) {
    checkArgument(canonicals.containsKey(canonicalKeysymMnemonic));
    return canonicals.get(canonicalKeysymMnemonic).nonDeprecatedAliases();
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
   * @return keyset contains a subset of the mnemonics, some of which might be deprecated
   */
  public ImmutableMap<String, Integer> ucpByMnemonic() {
    return ucp;
  }

  ImmutableMap<Integer, Integer> ucpByCode() {
    ImmutableSetMultimap<Integer, String> mnemonicsByCode = code.asMultimap().inverse();
    ImmutableMap<Integer, Set<Integer>> ucpsByCode = Maps.toMap(mnemonicsByCode.keySet(), c -> mnemonicsByCode.get(c).stream().filter(m -> ucp.containsKey(m)).map(m -> ucp.get(m)).distinct().collect(ImmutableSet.toImmutableSet()));
    Map<Integer, Set<Integer>> ucpsByCodeWithUcp = Maps.filterValues(ucpsByCode, s -> !s.isEmpty());
    return Maps.toMap(ucpsByCodeWithUcp.keySet(), c -> Iterables.getOnlyElement(ucpsByCodeWithUcp.get(c)));
  }

  public ImmutableSet<CanonicalMnemonic> asSet() {
    return canonicals.values();
  }

  public ImmutableBiMap<String, CanonicalMnemonic> asCanonicalMap() {
    return canonicals;
  }

  /**
   * This loses codes that are mapped only to deprecated mnemonics.
   */
  public Mnemonics withoutDeprecated() {
    Set<CanonicalMnemonic> nonDCan = canonicals.values().stream().filter(c -> !c.deprecated()).collect(ImmutableSet.toImmutableSet());
    ImmutableSet<CanonicalMnemonic> nonD = nonDCan.stream().map(Mnemonics::withoutDeprecatedAliases).collect(ImmutableSet.toImmutableSet());
    return new Mnemonics(nonD);
  }

  private static CanonicalMnemonic withoutDeprecatedAliases(CanonicalMnemonic canonicalMnemonic) {
    return new CanonicalMnemonic(canonicalMnemonic.mnemonic, canonicalMnemonic.code, canonicalMnemonic.nonDeprecatedAliases(), ImmutableSet.of(), canonicalMnemonic.ucp, canonicalMnemonic.deprecated);
  }
}
