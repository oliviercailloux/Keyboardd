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
 * Association of the mnemonics to their code and aliases and possibly UCP and whether they are deprecated.
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
    /*
     * Anyway, this is hopeless, I
     * suppose, as any ucp is automatically mapped to a code, which, I suppose, differs very often
     * from the mnemonic one. I’d better assume (reasonably, I suppose) that any X system will do
     * the same thing when facing two keysym codes that are standardly mapped to the same unicode
     * (such as the mnemonic “exclam” with keysym code 0x21 and the mnemonic absent with keysym code
     * 0x1000021 corresponding to U+0021 EXCLAMATION MARK), and thus not try to make ucps
     * distinguish these keysym codes.
 * <p>
 * This method patches the mnemonics to fix issue
 * <a href="https://github.com/xkbcommon/libxkbcommon/issues/433">#433</a>.
 * <p>
 * Multiple codes may may to a given ucp (eg mnemonic exclam, code 0x21, ucp U+0021 EXCLAMATION
 * MARK, and mnemonic absent, code 0x1000021, ucp U+0021).
 * 
 * Two pairs of mnemonics share a unicode point but different codes: radical, 0x08d6, U+221A SQUARE
 * ROOT (in Technical) and squareroot, 0x100221A, U+221A SQUARE ROOT; as well as partialderivative,
 * 0x08ef, U+2202 PARTIAL DIFFERENTIAL (in Technical) and partdifferential, 0x1002202, U+2202
 * PARTIAL DIFFERENTIAL (in XK_MATHEMATICAL). This class patches those by assigning squareroot,
 * 0x100221A, to no unicode and comment “2√”; and partdifferential, 0x1002202, to U+1D6DB
 * MATHEMATICAL BOLD PARTIAL DIFFERENTIAL.
 * 
 * With these two modifications, among non-deprecated values, we have that two entries with the same
 * present unicode point map to the same code.
 * 
 * /* Among all non-deprecated mns assigned to a given sym, if not empty [such as #define
 * XKB_KEY_topleftradical 0x08a2 /*(U+250C BOX DRAWINGS LIGHT DOWN AND RIGHT)], exactly one is not
 * an alias, and all others are aliases of that one.
 * 
 * Check: when mn1, mn2 to same code, then non first ones are either deprecated or comment equals
 * “alias for …”.
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

    public ImmutableSet<String> mnemonics() {
      return ImmutableSet.<String>builder().add(mnemonic).addAll(nonDeprecatedAliases)
          .addAll(deprecatedAliases).build();
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

  public static Mnemonics latestTODOOtherSource() {
  }
  
  public static Mnemonics latest() {
    ImmutableSet<ParsedMnemonic> parsedMns = KeysymReader.latest();
    ImmutableMap<ParsedMnemonic, Integer> mnToCode = parsedMns.stream().collect(ImmutableMap.toImmutableMap(m -> m, m -> m.code()));
    ImmutableSetMultimap<Integer, ParsedMnemonic> codeToMns = mnToCode.asMultimap().inverse();
    ImmutableSet<Integer> codes = codeToMns.keySet();
    ImmutableSet<CanonicalMnemonic> canonicals = codes.stream().map(c -> toCanonical(codeToMns.get(c))).collect(ImmutableSet.toImmutableSet());
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
    ImmutableSet<ParsedMnemonic> remaining = ImmutableSet.<ParsedMnemonic>builder().addAll(iterator).build();
    boolean deprecated = first.deprecated();
    if(deprecated) {
      checkArgument(mnemonics.stream().allMatch(ParsedMnemonic::deprecated));
    }
    int code = mnemonics.stream().map(ParsedMnemonic::code).distinct().collect(MoreCollectors.onlyElement());
    Optional<Integer> ucp = mnemonics.stream().map(ParsedMnemonic::unicode).distinct().collect(MoreCollectors.onlyElement());
    ImmutableSet<ParsedMnemonic> deprecateds = remaining.stream().filter(ParsedMnemonic::deprecated).collect(ImmutableSet.toImmutableSet());
    ImmutableSet<ParsedMnemonic> nonDeprecateds = remaining.stream().filter(p -> !p.deprecated()).collect(ImmutableSet.toImmutableSet());
    return new CanonicalMnemonic(first.mnemonic(), code, null, nonDeprecateds.stream().map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet()), ucp, deprecated);
    return new CanonicalMnemonic(first.mnemonic(), code, null, deprecateds.stream().map(ParsedMnemonic::mnemonic).collect(ImmutableSet.toImmutableSet()), ucp, deprecated);

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
