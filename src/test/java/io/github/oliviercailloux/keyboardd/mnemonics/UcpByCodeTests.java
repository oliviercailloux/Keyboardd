package io.github.oliviercailloux.keyboardd.mnemonics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UcpByCodeTests {
  @Test
  public void testImplicit() {
    UcpByCode ucpByCode = UcpByCode.implicit();
    assertEquals(0x100, ucpByCode.ucp(0x01_000_100));
    assertEquals(0x200, ucpByCode.ucp(0x01_000_200));
    assertEquals(0x10F_FFF, ucpByCode.ucp(0x01_10F_FFF));
  }
  @Test
  public void testLatest() {
    Mnemonics mns = Mnemonics.latest();
    UcpByCode ucpByCode = UcpByCode.withExplicit(mns);
    assertEquals(0x100, ucpByCode.ucp(0x01_000_100));
    assertEquals(0x200, ucpByCode.ucp(0x01_000_200));
    assertEquals(0x10F_FFF, ucpByCode.ucp(0x01_10F_FFF));
    assertEquals(' ', ucpByCode.ucp(mns.codeByMnemonic().get("space")));
    assertEquals('A', ucpByCode.ucp(mns.codeByMnemonic().get("A")));
  }
}
