package io.github.oliviercailloux.keyboardd.mapping;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableListMultimap.Builder;

import io.github.oliviercailloux.jaris.collections.CollectionUtils;

public class KeyboardMap {
  private final ImmutableListMultimap<String, KeysymEntry> xKeyNameToEntries;
  private ImmutableSetMultimap<String, String> mnemonicToXKeyNames;
  private ImmutableSetMultimap<Integer, String> ucpToXKeyNames;
  private ImmutableSetMultimap<Integer, String> codeToXKeyNames;

  public static KeyboardMap from(ListMultimap<String, KeysymEntry> xKeyNameToKeysymEntries) {
    return new KeyboardMap(xKeyNameToKeysymEntries);
  }

  private KeyboardMap(ListMultimap<String, KeysymEntry> xKeyNameToKeysymEntries) {
    this.xKeyNameToEntries = ImmutableListMultimap.copyOf(xKeyNameToKeysymEntries);
    mnemonicToXKeyNames = null;
    ucpToXKeyNames = null;
    codeToXKeyNames = null;
  }

  public ImmutableSet<String> names() {
    return xKeyNameToEntries.keySet();
  }

  public ImmutableList<KeysymEntry> entries(String xKeyName) {
    return xKeyNameToEntries.get(xKeyName);
  }

  public ImmutableListMultimap<String, KeysymEntry> nameToEntries() {
    return xKeyNameToEntries;
  }

  private void lazyInitReverse() {
    verify((mnemonicToXKeyNames == null) == (ucpToXKeyNames == null));
    verify((mnemonicToXKeyNames == null) == (codeToXKeyNames == null));

    if (mnemonicToXKeyNames == null) {
      ImmutableListMultimap<KeysymEntry, String> fromAllEntries = xKeyNameToEntries.inverse();
      ImmutableMap<KeysymEntry, Collection<String>> fromAllEntriesMap = fromAllEntries.asMap();
      {
        Map<KeysymEntry, Collection<String>> fromMnemonicEntriesMap =
            Maps.filterKeys(fromAllEntriesMap, e -> e.mnemonic().isPresent());
        ImmutableMap<String, Collection<String>> fromMnemonicMap =
            CollectionUtils.transformKeys(fromMnemonicEntriesMap, e -> e.mnemonic().get());
        mnemonicToXKeyNames = fromMnemonicMap.entrySet().stream().collect(ImmutableSetMultimap
            .flatteningToImmutableSetMultimap(e -> e.getKey(), e -> e.getValue().stream()));
      }
      {
        Map<KeysymEntry, Collection<String>> fromUcpEntriesMap =
            Maps.filterKeys(fromAllEntriesMap, e -> e.ucp().isPresent());
        ImmutableMap<Integer, Collection<String>> fromUcpMap =
            CollectionUtils.transformKeys(fromUcpEntriesMap, e -> e.ucp().get());
        ucpToXKeyNames = fromUcpMap.entrySet().stream().collect(ImmutableSetMultimap
            .flatteningToImmutableSetMultimap(e -> e.getKey(), e -> e.getValue().stream()));
      }
      {
        Map<KeysymEntry, Collection<String>> fromCodeEntriesMap =
            Maps.filterKeys(fromAllEntriesMap, e -> e.code().isPresent());
        ImmutableMap<Integer, Collection<String>> fromCodeMap =
            CollectionUtils.transformKeys(fromCodeEntriesMap, e -> e.code().get());
        codeToXKeyNames = fromCodeMap.entrySet().stream().collect(ImmutableSetMultimap
            .flatteningToImmutableSetMultimap(e -> e.getKey(), e -> e.getValue().stream()));
      }
    }
  }

  public ImmutableSet<String> namesFromMnemonic(String keysymMnemonic) {
    lazyInitReverse();
    return mnemonicToXKeyNames.get(keysymMnemonic);
  }

  public ImmutableSet<String> namesFromUcp(int ucp) {
    lazyInitReverse();
    return ucpToXKeyNames.get(ucp);
  }

  public ImmutableSet<String> namesFromCode(int keysymCode) {
    lazyInitReverse();
    return codeToXKeyNames.get(keysymCode);
  }

  public KeyboardMap canonicalize(Map<String, String> canonicalXKeyNameByAlias) {
    final ImmutableMap.Builder<String, String> newNameFromOriginalBuilder =
        new ImmutableMap.Builder<>();

    ImmutableSet<String> originalNames = xKeyNameToEntries.keySet();
    for (String originalName : originalNames) {
      String newName = canonicalXKeyNameByAlias.getOrDefault(originalName, originalName);
      newNameFromOriginalBuilder.put(originalName, newName);
    }
    ImmutableMap<String, String> newNameFromOriginal = newNameFromOriginalBuilder.build();
    ImmutableSetMultimap<String, String> originalsFromNewName =
        newNameFromOriginal.asMultimap().inverse();
    ImmutableSet<String> duplicatedNewNames =
        originalsFromNewName.keySet().stream().filter(n -> originalsFromNewName.get(n).size() >= 2)
            .collect(ImmutableSet.toImmutableSet());
    // originalsFromNewName.keySet().stream().filter(n -> originalsFromNewName.get(n).size() >= 2)
    // .collect(ImmutableSetMultimap.toImmutableSetMultimap(n -> originalsFromNewName));
    // checkState(duplicatedNewNames.isEmpty(), "These ");
    if (!duplicatedNewNames.isEmpty()) {
      final ImmutableSetMultimap.Builder<String, String> duplicatedMapBuilder =
          new ImmutableSetMultimap.Builder<>();
      for (String duplicatedNewName : duplicatedNewNames) {
        ImmutableSet<String> originals = originalsFromNewName.get(duplicatedNewName);
        duplicatedMapBuilder.putAll(duplicatedNewName, originals);
      }
      ImmutableSetMultimap<String, String> duplicatedMap = duplicatedMapBuilder.build();
      throw new IllegalStateException(
          "These new names are pointed to from several original names: %s.".formatted(duplicatedMap));
    }
    // ImmutableBiMap<String, String> originalFromNewName =
        ImmutableBiMap.copyOf(newNameFromOriginal).inverse();

    ImmutableListMultimap.Builder<String, KeysymEntry> withNewNames =
        ImmutableListMultimap.builder();
    for (String original : originalNames) {
      withNewNames.putAll(newNameFromOriginal.get(original), xKeyNameToEntries.get(original));
    }
    ImmutableListMultimap<String, KeysymEntry> withNewNamesBuilt = withNewNames.build();
    return KeyboardMap.from(withNewNamesBuilt);
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof KeyboardMap)) {
      return false;
    }
    final KeyboardMap t2 = (KeyboardMap) o2;
    return xKeyNameToEntries.equals(t2.xKeyNameToEntries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(xKeyNameToEntries);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("xKeyNameToEntries", xKeyNameToEntries).toString();
  }
}
