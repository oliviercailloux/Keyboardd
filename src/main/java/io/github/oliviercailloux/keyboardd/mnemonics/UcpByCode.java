package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.oliviercailloux.keyboardd.mnemonics.Mnemonics.CanonicalMnemonic;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class UcpByCode {
  public static final ContiguousSet<Integer> IMPLICIT_UCPS = ContiguousSet.closed(0x100, 0x10F_FFF);
  public static final ContiguousSet<Integer> IMPLICIT_UCP_KEYSYM_CODES =
      ContiguousSet.closed(0x01_000_100, 0x01_10F_FFF);

  public static UcpByCode implicit() {
    return new UcpByCode(ImmutableMap.of(), c -> c - 0x01_000_000, IMPLICIT_UCP_KEYSYM_CODES,
        IMPLICIT_UCPS);
  }

  public static UcpByCode implicitAndExplicit(Map<Integer, Integer> ucpByCodeExplicit) {
    ContiguousSet<Integer> domain = IMPLICIT_UCP_KEYSYM_CODES;
    ImmutableSet.Builder<Integer> domainBuilder = ImmutableSet.builder();

    ContiguousSet<Integer> coDomain = IMPLICIT_UCPS;
    ImmutableSet.Builder<Integer> coDomainBuilder = ImmutableSet.builder();

    return new UcpByCode(ucpByCodeExplicit, c -> c - 0x01_000_000,
        domainBuilder.addAll(domain).addAll(ucpByCodeExplicit.keySet()).build(),
        coDomainBuilder.addAll(coDomain).addAll(ucpByCodeExplicit.values()).build());
  }

  public static UcpByCode implicitAndExplicit(Mnemonics mnemonics) {
    ImmutableBiMap<Integer, CanonicalMnemonic> byUcp = mnemonics.byUcp();
    ImmutableBiMap<Integer, Integer> codeByUcp = byUcp.keySet().stream()
        .collect(ImmutableBiMap.toImmutableBiMap(u -> u, u -> byUcp.get(u).code()));
    return implicitAndExplicit(codeByUcp.inverse());
  }

  private final ImmutableMap<Integer, Integer> ucpByCodeExplicit;
  private final Function<Integer, Integer> ucpByCodeImplicit;
  private final ImmutableSet<Integer> domainOfCodes;
  private final ImmutableSet<Integer> coDomainOfUcps;

  private UcpByCode(Map<Integer, Integer> ucpByCodeExplicit,
      Function<Integer, Integer> ucpByCodeImplicit, Set<Integer> domain, Set<Integer> coDomain) {
    checkArgument(domain.size() >= coDomain.size());

    this.ucpByCodeExplicit = ImmutableMap.copyOf(ucpByCodeExplicit);
    this.ucpByCodeImplicit = ucpByCodeImplicit;
    checkArgument(domain.containsAll(ucpByCodeExplicit.keySet()));
    this.domainOfCodes = ImmutableSet.copyOf(domain);
    checkArgument(coDomain.containsAll(ucpByCodeExplicit.values()));
    this.coDomainOfUcps = ImmutableSet.copyOf(coDomain);
  }

  public ImmutableSet<Integer> domainOfCodes() {
    return domainOfCodes;
  }

  public ImmutableSet<Integer> coDomainOfUcps() {
    return coDomainOfUcps;
  }

  public int ucp(int code) {
    checkArgument(domainOfCodes.contains(code));
    if (ucpByCodeExplicit.containsKey(code)) {
      return ucpByCodeExplicit.get(code);
    }
    int ucp = ucpByCodeImplicit.apply(code);
    verify(coDomainOfUcps.contains(ucp));
    return ucp;
  }
}
