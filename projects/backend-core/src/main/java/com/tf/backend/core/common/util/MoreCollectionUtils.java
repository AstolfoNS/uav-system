package com.tf.backend.core.common.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MoreCollectionUtils {

    private MoreCollectionUtils() {}

    public static <T, K> Map<K, T> indexByStrict(
            Collection<T> list,
            Function<T, K> keyExtractor,
            Supplier<String> nullListMsg,
            Function<K, String> nullKeyMsg,
            Function<K, String> duplicateKeyMsg
    ) {
        if (list == null) {
            throw new IllegalStateException(nullListMsg.get());
        }
        Map<K, T> map = new HashMap<>(Math.max(16, list.size() * 2));
        for (T item : list) {
            K key = keyExtractor.apply(item);
            if (key == null) {
                throw new IllegalStateException(nullKeyMsg.apply(null));
            }
            T prev = map.putIfAbsent(key, item);
            if (prev != null) {
                throw new IllegalStateException(duplicateKeyMsg.apply(key));
            }
        }
        return map;
    }

    // 更常用的简化重载：给 entityName 就够了
    public static <T, K> Map<K, T> indexByStrict(
            Collection<T> list,
            Function<T, K> keyExtractor,
            String entityName
    ) {
        return indexByStrict(
                list,
                keyExtractor,
                () -> "No " + entityName + " records returned",
                _ -> entityName + " id is null",
                key -> "Duplicate " + entityName + " key: " + key
        );
    }

    public static <T, K> Set<K> collectNonNullToSet(
            Collection<T> list,
            Function<T, K> extractor,
            Supplier<String> nullValueMsg
    ) {
        if (list == null) {
            return Collections.emptySet();
        }
        Set<K> set = new HashSet<>();
        for (T item : list) {
            K v = extractor.apply(item);
            if (v == null) {
                throw new IllegalStateException(nullValueMsg.get());
            }
            set.add(v);
        }
        return set;
    }

    public static <K, V> V requirePresent(
            Map<K, V> map,
            K key,
            Supplier<String> errorMsg
    ) {
        V v = map.get(key);
        if (v == null) {
            throw new IllegalStateException(errorMsg.get());
        }
        return v;
    }

    public static <T> T requireNonNull(T v, Supplier<String> msg) {
        if (v == null) {
            throw new IllegalStateException(msg.get());
        }
        return v;
    }

    public static <T, K> Set<K> collectNonNullToSet(
            Collection<T> list,
            Function<T, K> extractor,
            String nullValueMsg
    ) {
        return collectNonNullToSet(list, extractor, () -> nullValueMsg);
    }

    public static <K, V> V requirePresent(
            Map<K, V> map,
            K key,
            String errorMsg
    ) {
        return requirePresent(map, key, () -> errorMsg);
    }
}
