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
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import io.github.oliviercailloux.keyboardd.mnemonics.KeysymReader.ParsedMnemonic;

/**
 * Association of the mnemonics to their code and aliases and possibly UCP and whether they are
 * deprecated.
 * <ul>
 * <li>Maps each keysym mnemonic to exactly one keysym code.</li>
 * <li>Indicates, for each keysym mnemonic, whether it is deprecated.</li>
 * <li>Maps each keysym mnemonic to its canonical and aliases relatives (meaning the keysym
 * mnemonics that are associated to the same code).</li>
 * <li>Maps each keysym code to at most one keysym mnemonic. If that keysym mnemonic is deprecated,
 * then all its aliases are as well.</li>
 * <li>Maps each keysym mnemonic to at most one UCP.</li>
 * <li>Maps each UCP to at most one canonical keysym mnemonic.</li>
 * </ul>
 * <p>
 */
public class Mnemonics {
  public static record CanonicalMnemonic (String mnemonic, int code,
      ImmutableSet<String> nonDeprecatedAliases, ImmutableSet<String> deprecatedAliases,
      Optional<Integer> ucp, boolean deprecated) {
    public CanonicalMnemonic {
      checkArgument(!mnemonic.isEmpty());
      checkArgument(Sets.intersection(nonDeprecatedAliases, deprecatedAliases).isEmpty());
      if (deprecated) {
        checkArgument(nonDeprecatedAliases.isEmpty());
      }
    }

    /**
     * Retrieves the mnemonics associated to the same code as this one.
     * @return the relative mnemonics, starting with this one, followed by the non deprecated aliases and finally by the deprecated aliases.
     */
    public ImmutableSet<String> mnemonics() {
      return ImmutableSet.<String>builder().add(mnemonic).addAll(nonDeprecatedAliases)
          .addAll(deprecatedAliases).build();
    }

    /**
     * Retrieves the mnemonics associated to the same code as this one, except for this one.
     * @return the non deprecated aliases followed by the deprecated aliases.
     */
    public ImmutableSet<String> aliases() {
      return ImmutableSet.<String>builder().addAll(nonDeprecatedAliases).addAll(deprecatedAliases)
          .build();
    }
  }

  private final ImmutableMap<String, Integer> code;
  private final ImmutableMap<String, Integer> ucp;
  private final ImmutableBiMap<String, CanonicalMnemonic> canonicals;
  private final ImmutableMap<String, CanonicalMnemonic> canonicalsByAlias;

  /**
   * If the given source maps several codes (through different mnemonics) to a given UCP, it must
   * have exactly one of these codes in the range UcpByCode#IMPLICIT_UCP_KEYSYM_CODES. Only the
   * mnemonics associated to that code will be considered associated to that UCP, not the mnemonics
   * associated to other codes.
   */
  public static Mnemonics latestTODOOtherSource() {}

  public static Mnemonics latest() {
    ImmutableSet<ParsedMnemonic> parsedMns = KeysymReader.latest();
    ImmutableMap<ParsedMnemonic, Integer> mnToCode =
        parsedMns.stream().collect(ImmutableMap.toImmutableMap(m -> m, m -> m.code()));
    ImmutableSetMultimap<Integer, ParsedMnemonic> codeToMns = mnToCode.asMultimap().inverse();
    ImmutableSet<Integer> codes = codeToMns.keySet();
    ImmutableSet<CanonicalMnemonic> canonicals = codes.stream()
        .map(c -> toCanonical(codeToMns.get(c))).collect(ImmutableSet.toImmutableSet());
    ImmutableBiMap<String, ParsedMnemonic> canonicalMns =
        patchedMns.stream().filter(m -> !m.alias())
            .collect(ImmutableBiMap.toImmutableBiMap(ParsedMnemonic::mnemonic, m -> m));
    ImmutableSetMultimap<String, ParsedMnemonic> aliasesByCanonical =
        patchedMns.stream().filter(m -> m.alias()).collect(
            ImmutableSetMultimap.toImmutableSetMultimap(ParsedMnemonic::remainingComment, m -> m));
    // ImmutableSetMultimap<ParsedMnemonic, ParsedMnemonic> aliasesByCanonical =
    // patchedMns.stream().filter(m ->
    // m.alias()).collect(ImmutableSetMultimap.toImmutableSetMultimap(m ->
    // canonicalMns.get(m.remainingComment()), m -> m));
    ImmutableSet.Builder<CanonicalMnemonic> canonicalMnemonicsBuilder = ImmutableSet.builder();
    for (ParsedMnemonic mn : canonicalMns.values()) {
      String canonical = mn.mnemonic();
      ImmutableSet<ParsedMnemonic> allAliases = aliasesByCanonical.get(canonical);
      ImmutableSet<String> nonDeprecatedaliases = allAliases.stream().filter(m -> !m.deprecated())
          .map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet());
      ImmutableSet<String> deprecatedAliases =
          allAliases.stream().filter(ParsedMnemonic::deprecated).map(ParsedMnemonic::mnemonic)
              .collect(ImmutableSet.toImmutableSet());
      CanonicalMnemonic canonicalMnemonic = new CanonicalMnemonic(canonical, mn.code(),
          nonDeprecatedaliases, deprecatedAliases, mn.unicode(), mn.deprecated());
      canonicalMnemonicsBuilder.add(canonicalMnemonic);
    }
    return new Mnemonics(canonicalMnemonicsBuilder.build());
  }

  private static CanonicalMnemonic toCanonical(ImmutableSet<ParsedMnemonic> mnemonics) {
    UnmodifiableIterator<ParsedMnemonic> iterator = mnemonics.iterator();
    checkArgument(iterator.hasNext());
    ParsedMnemonic first = iterator.next();
    ImmutableSet<ParsedMnemonic> remaining =
        ImmutableSet.<ParsedMnemonic>builder().addAll(iterator).build();
    boolean deprecated = first.deprecated();
    if (deprecated) {
      checkArgument(mnemonics.stream().allMatch(ParsedMnemonic::deprecated));
    }
    int code = mnemonics.stream().map(ParsedMnemonic::code).distinct()
        .collect(MoreCollectors.onlyElement());
    Optional<Integer> ucp = mnemonics.stream().map(ParsedMnemonic::unicode).distinct()
        .collect(MoreCollectors.onlyElement());
    ImmutableSet<ParsedMnemonic> deprecateds = remaining.stream().filter(ParsedMnemonic::deprecated)
        .collect(ImmutableSet.toImmutableSet());
    ImmutableSet<ParsedMnemonic> nonDeprecateds =
        remaining.stream().filter(p -> !p.deprecated()).collect(ImmutableSet.toImmutableSet());
    return new CanonicalMnemonic(first.mnemonic(), code, null, nonDeprecateds.stream()
        .map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet()), ucp, deprecated);
    return new CanonicalMnemonic(first.mnemonic(), code, null,
        deprecateds.stream().map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet()),
        ucp, deprecated);

  }

  public static Mnemonics from(Set<CanonicalMnemonic> canonicalMnemonics) {
    return new Mnemonics(canonicalMnemonics);
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

    final ImmutableMap.Builder<String, Integer> ucpByKeysymMnemonicBuilder = ImmutableMap.builder();
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

    if (isCanonical) {
      CanonicalMnemonic canonical;
      canonical = canonicals.get(keysymMnemonic);
      return canonical.deprecated();
    }
    CanonicalMnemonic canonical;
    canonical = canonicalsByAlias.get(keysymMnemonic);
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
    ImmutableMap<Integer,
        Set<Integer>> ucpsByCode = Maps.toMap(mnemonicsByCode.keySet(),
            c -> mnemonicsByCode.get(c).stream().filter(m -> ucp.containsKey(m))
                .map(m -> ucp.get(m)).distinct().collect(ImmutableSet.toImmutableSet()));
    Map<Integer, Set<Integer>> ucpsByCodeWithUcp = Maps.filterValues(ucpsByCode, s -> !s.isEmpty());
    return Maps.toMap(ucpsByCodeWithUcp.keySet(),
        c -> Iterables.getOnlyElement(ucpsByCodeWithUcp.get(c)));
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
    Set<CanonicalMnemonic> nonDCan = canonicals.values().stream().filter(c -> !c.deprecated())
        .collect(ImmutableSet.toImmutableSet());
    ImmutableSet<CanonicalMnemonic> nonD = nonDCan.stream().map(Mnemonics::withoutDeprecatedAliases)
        .collect(ImmutableSet.toImmutableSet());
    return new Mnemonics(nonD);
  }

  private static CanonicalMnemonic withoutDeprecatedAliases(CanonicalMnemonic canonicalMnemonic) {
    return new CanonicalMnemonic(canonicalMnemonic.mnemonic, canonicalMnemonic.code,
        canonicalMnemonic.nonDeprecatedAliases(), ImmutableSet.of(), canonicalMnemonic.ucp,
        canonicalMnemonic.deprecated);
  }
}
