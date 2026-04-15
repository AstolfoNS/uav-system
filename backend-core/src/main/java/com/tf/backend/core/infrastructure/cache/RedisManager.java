package com.tf.backend.core.infrastructure.cache;

import com.tf.backend.core.common.constant.RedisConst;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisManager {

    private final StringRedisTemplate redis;

    private final JsonMapper mapper;

    /**
     * 单体项目：本地防击穿锁（key 粒度）
     */
    private static final ConcurrentHashMap<String, Object> KEY_LOCKS = new ConcurrentHashMap<>();

    // ========================= 基础操作 =========================

    public boolean hasKey(@NotNull String key) {
        validateKey(key);
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public boolean delete(@NotNull String key) {
        validateKey(key);
        return Boolean.TRUE.equals(redis.delete(key));
    }

    public long delete(@NotNull Collection<String> keys) {
        return CollectionUtils.isEmpty(keys) ? 0 : Optional.ofNullable(redis.delete(keys)).orElse(0L);
    }

    public void expire(@NotNull String key, long timeout, @NotNull TimeUnit unit) {
        validateKey(key);
        redis.expire(key, timeout, unit);
    }

    public Set<String> keys(@NotNull String pattern) {
        return Optional.ofNullable(redis.keys(pattern)).orElse(Collections.emptySet());
    }

    // ========================= KV 操作 =========================

    public <T> void set(@NotNull String key, @NotNull T value) {
        set(key, value, RedisConst.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    public <T> void set(@NotNull String key, @NotNull T value, long timeout, @NotNull TimeUnit unit) {
        validateKey(key);
        Optional.ofNullable(toJson(value))
                .ifPresent(json -> redis.opsForValue().set(key, json, timeout, unit));
    }

    @Nullable
    public <T> T get(@NotNull String key, @NotNull Class<T> clazz) {
        validateKey(key);
        String json = redis.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        T val = fromJson(json, clazz);
        if (val == null) {
            deleteQuietly(key); // 序列化失败时清理脏数据
        }
        return val;
    }

    /**
     * 新增：支持复杂泛型的 get (例如 Page<User>)
     */
    @Nullable
    public <T> T get(@NotNull String key, @NotNull TypeReference<T> typeReference) {
        validateKey(key);
        String json = redis.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        T val = fromJson(json, typeReference);
        if (val == null) {
            deleteQuietly(key);
        }
        return val;
    }

    // ========================= getOrSet (核心防击穿逻辑) =========================

    public <T> T getOrSet(@NotNull String key, @NotNull Class<T> clazz, @NotNull Supplier<T> supplier) {
        return getOrSet(key, clazz, supplier, RedisConst.DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    public <T> T getOrSet(
            @NotNull String key,
            @NotNull Class<T> clazz,
            @NotNull Supplier<T> supplier,
            long timeout,
            @NotNull TimeUnit unit
    ) {
        validateKey(key);

        T cached = get(key, clazz);
        if (cached != null) {
            return cached;
        }
        if (isNullCached(key)) {
            return null;
        }
        Object lock = KEY_LOCKS.computeIfAbsent(key, _ -> new Object());
        synchronized (lock) {
            try {
                // 双重检查
                T cached2 = get(key, clazz);
                if (cached2 != null) {
                    return cached2;
                }
                if (isNullCached(key)) {
                    return null;
                }
                // 回源查询
                T newValue = supplier.get();

                if (newValue != null) {
                    set(key, newValue, timeout, unit);
                } else {
                    setNullValueCache(key);
                }
                return newValue;
            } catch (RuntimeException e) {
                // 修复：不要吞没异常！如果是 BizException 或 SQL 异常，应该让它抛出给全局处理器
                log.error("Execute supplier error: key={}, error={}", key, e.getMessage(), e);
                throw e;
            } finally {
                KEY_LOCKS.remove(key, lock);
            }
        }
    }

    // ========================= 动态 TTL（按返回值） =========================

    public <T> @Nullable T getOrSetDynamicTtl(
            @NotNull String key,
            @NotNull Class<T> clazz,
            @NotNull Supplier<T> supplier,
            @NotNull Function<T, Duration> ttlFunc,
            boolean cacheNull
    ) {
        validateKey(key);

        T cached = get(key, clazz);
        if (cached != null) {
            return cached;
        }
        if (cacheNull && isNullCached(key)) {
            return null;
        }
        Object lock = KEY_LOCKS.computeIfAbsent(key, _ -> new Object());
        synchronized (lock) {
            try {
                T cached2 = get(key, clazz);
                if (cached2 != null) {
                    return cached2;
                }
                if (cacheNull && isNullCached(key)) {
                    return null;
                }
                T val = supplier.get();
                if (val == null) {
                    if (cacheNull){
                        setNullValueCache(key);
                    }
                    return null;
                }

                Duration ttl = safeTtl(ttlFunc.apply(val));
                if (ttl == null) {
                    set(key, val);
                } else {
                    set(key, val, ttl.toMillis(), TimeUnit.MILLISECONDS);
                }
                return val;
            } catch (RuntimeException e) {
                log.error("getOrSetDynamicTtl supplier error: key={}, err={}", key, e.getMessage(), e);
                throw e;
            } finally {
                KEY_LOCKS.remove(key, lock);
            }
        }
    }

    // ========================= List 操作 =========================

    public <T> long setListOverwrite(@NotNull String key, @NotNull List<T> list) {
        validateKey(key);
        deleteQuietly(key);
        deleteQuietly(emptyMarkerKey(key));

        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        List<String> jsonList = list.stream()
                .map(this::toJson)
                .filter(Objects::nonNull)
                .toList();
        if (jsonList.isEmpty()) {
            return 0;
        }
        redis.opsForList().rightPushAll(key, jsonList);
        return jsonList.size();
    }

    public <T> long setListOverwrite(@NotNull String key, @NotNull List<T> list, long timeout, @NotNull TimeUnit unit) {
        long n = setListOverwrite(key, list);
        if (n > 0){
            expire(key, timeout, unit);
        }
        return n;
    }

    @NotNull
    public <T> List<T> getList(@NotNull String key, @NotNull Class<T> clazz) {
        validateKey(key);
        if (Boolean.TRUE.equals(redis.hasKey(emptyMarkerKey(key)))) {
            return Collections.emptyList();
        }
        List<String> jsonList = redis.opsForList().range(key, 0, -1);
        if (CollectionUtils.isEmpty(jsonList)) {
            return Collections.emptyList();
        }
        return jsonList.stream()
                .map(json -> fromJson(json, clazz))
                .filter(Objects::nonNull)
                .toList();
    }

    public <E> @NotNull List<E> getOrSetList(
            @NotNull String key,
            @NotNull Class<E> elementClass,
            @NotNull Supplier<List<E>> supplier,
            long timeout,
            @NotNull TimeUnit unit,
            boolean cacheEmpty
    ) {
        validateKey(key);
        List<E> cached = getListIfPresent(key, elementClass);
        if (cached != null) {
            return cached;
        }
        Object lock = KEY_LOCKS.computeIfAbsent(key, _ -> new Object());
        synchronized (lock) {
            try {
                List<E> cached2 = getListIfPresent(key, elementClass);
                if (cached2 != null) {
                    return cached2;
                }
                List<E> loaded = supplier.get();
                if (loaded == null) {
                    loaded = Collections.emptyList();
                }
                if (loaded.isEmpty()) {
                    if (cacheEmpty) setEmptyMarker(key, timeout, unit);
                    return Collections.emptyList();
                }
                setListOverwrite(key, loaded, timeout, unit);
                return loaded;
            } catch (RuntimeException e) {
                log.error("getOrSetList supplier error: key={}, err={}", key, e.getMessage(), e);
                throw e;
            } finally {
                KEY_LOCKS.remove(key, lock);
            }
        }
    }

    @Nullable
    private <E> List<E> getListIfPresent(@NotNull String key, @NotNull Class<E> elementClass) {
        try {
            if (Boolean.TRUE.equals(redis.hasKey(emptyMarkerKey(key)))) {
                return Collections.emptyList();
            }
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                return null;
            }
            return getList(key, elementClass);
        } catch (Exception e) {
            log.error("getListIfPresent error: key={}, err={}", key, e.getMessage(), e);
            return null;
        }
    }

    // ========================= Set 操作 =========================

    public <T> long setSetOverwrite(@NotNull String key, @NotNull Set<T> set) {
        validateKey(key);
        deleteQuietly(key);
        deleteQuietly(emptyMarkerKey(key));

        if (CollectionUtils.isEmpty(set)){
            return 0;
        }
        String[] jsons = set.stream()
                .map(this::toJson)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(String[]::new);

        if (jsons.length == 0) {
            return 0;
        }
        return Optional.ofNullable(redis.opsForSet().add(key, jsons)).orElse(0L);
    }

    public <T> long setSetOverwrite(@NotNull String key, @NotNull Set<T> set, long timeout, @NotNull TimeUnit unit) {
        long n = setSetOverwrite(key, set);
        if (n > 0) {
            expire(key, timeout, unit);
        }
        return n;
    }

    @NotNull
    public <T> Set<T> getSet(@NotNull String key, @NotNull Class<T> clazz) {
        validateKey(key);
        if (Boolean.TRUE.equals(redis.hasKey(emptyMarkerKey(key)))) {
            return Collections.emptySet();
        }
        Set<String> jsonSet = redis.opsForSet().members(key);
        if (CollectionUtils.isEmpty(jsonSet)) {
            return Collections.emptySet();
        }
        return jsonSet.stream()
                .map(json -> fromJson(json, clazz))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public <E> @NotNull Set<E> getOrSetSet(
            @NotNull String key,
            @NotNull Class<E> elementClass,
            @NotNull Supplier<Set<E>> supplier,
            long timeout,
            @NotNull TimeUnit unit,
            boolean cacheEmpty
    ) {
        validateKey(key);
        Set<E> cached = getSetIfPresent(key, elementClass);
        if (cached != null) {
            return cached;
        }
        Object lock = KEY_LOCKS.computeIfAbsent(key, _ -> new Object());
        synchronized (lock) {
            try {
                Set<E> cached2 = getSetIfPresent(key, elementClass);
                if (cached2 != null) {
                    return cached2;
                }
                Set<E> loaded = supplier.get();
                if (loaded == null) {
                    loaded = Collections.emptySet();
                }
                if (loaded.isEmpty()) {
                    if (cacheEmpty) setEmptyMarker(key, timeout, unit);
                    return Collections.emptySet();
                }
                setSetOverwrite(key, loaded, timeout, unit);
                return loaded;
            } catch (RuntimeException e) {
                log.error("getOrSetSet supplier error: key={}, err={}", key, e.getMessage(), e);
                throw e;
            } finally {
                KEY_LOCKS.remove(key, lock);
            }
        }
    }

    @Nullable
    private <E> Set<E> getSetIfPresent(@NotNull String key, @NotNull Class<E> elementClass) {
        try {
            if (Boolean.TRUE.equals(redis.hasKey(emptyMarkerKey(key)))) {
                return Collections.emptySet();
            }
            if (!Boolean.TRUE.equals(redis.hasKey(key))) {
                return null;
            }
            return getSet(key, elementClass);
        } catch (Exception e) {
            log.error("getSetIfPresent error: key={}, err={}", key, e.getMessage(), e);
            return null;
        }
    }

    // ========================= Hash 操作 =========================

    public <T> void putHash(@NotNull String key, @NotNull String hashKey, @NotNull T value) {
        validateKey(key);
        validateKey(hashKey);
        Optional.ofNullable(toJson(value))
                .ifPresent(json -> redis.opsForHash().put(key, hashKey, json));
    }

    @Nullable
    public <T> T getHashValue(@NotNull String key, @NotNull String hashKey, @NotNull Class<T> clazz) {
        validateKey(key);
        validateKey(hashKey);
        Object json = redis.opsForHash().get(key, hashKey);
        if (json == null) {
            return null;
        }
        T val = fromJson(json.toString(), clazz);
        if (val == null) {
            deleteHashKey(key, hashKey);
        }
        return val;
    }

    public void deleteHashKey(@NotNull String key, @NotNull String hashKey) {
        validateKey(key);
        validateKey(hashKey);
        redis.opsForHash().delete(key, hashKey);
    }

    /**
     * 向 Hash 中存入原生字符串 (不走 JSON 序列化)
     * 非常适合高频存储时间戳、简单状态标识等
     */
    public void hPut(@NotNull String key, @NotNull String hashKey, @NotNull String value) {
        validateKey(key);
        validateKey(hashKey);
        // 直接存入原生字符串
        redis.opsForHash().put(key, hashKey, value);
    }

    /**
     * 获取 Hash 中的所有键值对
     * 配合 hPut 使用，返回的是底层的原生 Map 结构
     */
    @NotNull
    public Map<Object, Object> hGetAll(@NotNull String key) {
        validateKey(key);
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        return entries != null ? entries : Collections.emptyMap();
    }

    // ========================= 内部工具 =========================

    private void validateKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be null or empty!");
        }
    }

    private void setNullValueCache(@NotNull String key) {
        redis.opsForValue().set(RedisConst.NULL_CACHE_PREFIX.concat(key), "NULL", Duration.ofMinutes(RedisConst.NULL_CACHE_TTL_MINUTES));
    }

    private boolean isNullCached(@NotNull String key) {
        return Boolean.TRUE.equals(redis.hasKey(RedisConst.NULL_CACHE_PREFIX.concat(key)));
    }

    private void deleteQuietly(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete cache: key={}, err={}", key, e.getMessage());
        }
    }

    private String emptyMarkerKey(String key) {
        return key + RedisConst.EMPTY_MARKER_SUFFIX;
    }

    private void setEmptyMarker(@NotNull String key, long timeout, @NotNull TimeUnit unit) {
        redis.opsForValue().set(emptyMarkerKey(key), "1", timeout, unit);
    }

    private Duration safeTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) return null;
        return ttl;
    }

    private <T> String toJson(T value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("Redis JSON Serialization failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Redis JSON Deserialization failed for type {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("Redis JSON Deserialization failed for typeReference: {}", e.getMessage());
            return null;
        }
    }
}
