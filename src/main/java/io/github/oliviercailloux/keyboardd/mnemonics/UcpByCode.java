package io.github.oliviercailloux.keyboardd.mnemonics;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

public class UcpByCode {
  public static UcpByCode implicit() {
    return new UcpByCode(ImmutableMap.of(), c -> c - 0x01_000_000,
        ContiguousSet.create(Range.closed(0x01_000_100, 0x01_10F_FFF), DiscreteDomain.integers()),
        ContiguousSet.create(Range.closed(0x100, 0x10F_FFF), DiscreteDomain.integers()));
  }

  public static UcpByCode implicitAndExplicit(Map<Integer, Integer> ucpByCodeExplicit) {
    ContiguousSet<Integer> domain = ContiguousSet.create(Range.closed(0x01_000_100, 0x01_10F_FFF), DiscreteDomain.integers());
    ImmutableSet.Builder<Integer> domainBuilder = ImmutableSet.builder();
    
    ContiguousSet<Integer> coDomain = ContiguousSet.create(Range.closed(0x100, 0x10F_FFF), 
    DiscreteDomain.integers());
    ImmutableSet.Builder<Integer> coDomainBuilder = ImmutableSet.builder();

    return new UcpByCode(ucpByCodeExplicit, c -> c - 0x01_000_000,
        domainBuilder.addAll(domain).addAll(ucpByCodeExplicit.keySet()).build(),
        coDomainBuilder.addAll(coDomain).addAll(ucpByCodeExplicit.values()).build());
  }

  public static UcpByCode implicitAndExplicit(Mnemonics mnemonics) {
    return implicitAndExplicit(mnemonics.ucpByCode());
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

  public boolean hasUcp(int code) {
    return domainOfCodes.contains(code);
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
