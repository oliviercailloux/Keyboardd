package io.github.oliviercailloux.keyboardd.xkeys;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class XkeysImpl implements Xkeys {
  private final ImmutableBiMap<String, Short> codeByCanonical;
  private final ImmutableMap<String, String> canonicalByAlias;

  /**
   * 
   * @param codes
   * @param canonicalByAlias keys are all aliases, values are only canonicals
   * @return
   */
  public static Xkeys fromMaps(Map<String, Short> codes, Map<String, String> canonicalByAlias) {
    return new XkeysImpl(codes, canonicalByAlias);
  }

  XkeysImpl(Map<String, Short> codes, Map<String, String> canonicalByAlias) {
    this.codeByCanonical = ImmutableBiMap.copyOf(codes);
    this.canonicalByAlias = ImmutableMap.copyOf(canonicalByAlias);
    checkArgument(codeByCanonical.keySet().containsAll(canonicalByAlias.values()));
    checkArgument(Sets.intersection(codeByCanonical.keySet(), canonicalByAlias.keySet()).isEmpty());
  }

  @Override
  public ImmutableSet<Short> codes() {
    return codeByCanonical.values();
  }

  @Override
  public ImmutableSet<String> canonicals() {
    return codeByCanonical.keySet();
  }

  @Override
  public String canonical(short code) {
    checkArgument(codes().contains(code));
    return codeByCanonical.inverse().get(code);
  }

  @Override
  public ImmutableBiMap<String, Short> codeByCanonical() {
    return codeByCanonical;
  }

  @Override
  public ImmutableBiMap<Short, String> canonicalByCode() {
    return codeByCanonical.inverse();
  }

  @Override
  public ImmutableSet<String> aliases() {
    return canonicalByAlias.keySet();
  }

  @Override
  public ImmutableSet<String> aliases(String canonical) {
    checkArgument(canonicals().contains(canonical));
    return canonicalByAlias.asMultimap().inverse().get(canonical);
  }

  @Override
  public ImmutableMap<String, String> canonicalByAlias() {
    return canonicalByAlias;
  }

  @Override
  public ImmutableSet<String> names() {
    return Sets.union(canonicals(), aliases()).immutableCopy();
  }

  @Override
  public boolean isAlias(String keyName) {
    checkArgument(aliases().contains(keyName) || canonicals().contains(keyName));
    return aliases().contains(keyName);
  }

  @Override
  public String canonical(String keyName) {
    if (canonicals().contains(keyName))
      return keyName;
    checkArgument(aliases().contains(keyName));
    return canonicalByAlias.get(keyName);
  }
  
    @Override
    public short code(String keyName) {
      return codeByCanonical.get(canonical(keyName));
    }

  @Override
  public ImmutableSet<String> names(short code) {
    checkArgument(codes().contains(code));
    String canonical = canonicalByCode().get(code);
    ImmutableSet<String> aliases = aliases(canonical);
    final ImmutableSet.Builder<String> all = new ImmutableSet.Builder<String>().add(canonical).addAll(aliases);
    return all.build();
  }

  @Override
  public ImmutableMap<String, Short> codeByName() {
    return Maps.toMap(names(), this::code);
  }
}
