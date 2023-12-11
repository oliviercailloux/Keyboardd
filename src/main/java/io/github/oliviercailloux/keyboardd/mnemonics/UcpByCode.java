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

  public static UcpByCode withExplicit(Map<Integer, Integer> ucpByCodeExplicit) {
    ContiguousSet<Integer> domain = ContiguousSet.create(Range.closed(0x01_000_100, 0x01_10F_FFF), DiscreteDomain.integers());
    ImmutableSet.Builder<Integer> domainBuilder = ImmutableSet.builder();
    
    ContiguousSet<Integer> range = ContiguousSet.create(Range.closed(0x100, 0x10F_FFF), 
    DiscreteDomain.integers());
    ImmutableSet.Builder<Integer> rangeBuilder = ImmutableSet.builder();

    return new UcpByCode(ucpByCodeExplicit, c -> c - 0x01_000_000,
        domainBuilder.addAll(domain).addAll(ucpByCodeExplicit.keySet()).build(),
        rangeBuilder.addAll(range).addAll(ucpByCodeExplicit.values()).build());
  }

  public static UcpByCode withExplicit(Mnemonics mnemonics) {
    return withExplicit(mnemonics.ucpByCode());
  }

  private final ImmutableMap<Integer, Integer> ucpByCodeExplicit;
  private final Function<Integer, Integer> ucpByCodeImplicit;
  private final ImmutableSet<Integer> domainOfCodes;
  private final ImmutableSet<Integer> rangeOfUcps;

  private UcpByCode(Map<Integer, Integer> ucpByCodeExplicit,
      Function<Integer, Integer> ucpByCodeImplicit, Set<Integer> domain, Set<Integer> range) {
    checkArgument(domain.size() >= range.size());

    this.ucpByCodeExplicit = ImmutableMap.copyOf(ucpByCodeExplicit);
    this.ucpByCodeImplicit = ucpByCodeImplicit;
    checkArgument(domain.containsAll(ucpByCodeExplicit.keySet()));
    this.domainOfCodes = ImmutableSet.copyOf(domain);
    checkArgument(range.containsAll(ucpByCodeExplicit.values()));
    this.rangeOfUcps = ImmutableSet.copyOf(range);
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
    verify(rangeOfUcps.contains(ucp));
    return ucp;
  }
}
