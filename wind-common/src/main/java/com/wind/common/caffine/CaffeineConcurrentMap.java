package com.wind.common.caffine;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * caffeine map
 *
 * @param <K> key
 * @param <V> value
 * @author wuxp
 * @date 2026-02-24 14:40
 **/
@AllArgsConstructor
public class CaffeineConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    private final Cache<K, V> delegate;

    public CaffeineConcurrentMap(@NonNull CaffeineSpec spec) {
        delegate = Caffeine.from(spec).build();
    }

    @NonNull
    public static <K, V> CaffeineConcurrentMap<K, V> expireAfterWrite(@NonNull Duration duration) {
        Cache<K, V> builder = Caffeine.newBuilder().expireAfterWrite(duration).build();
        return new CaffeineConcurrentMap<>(builder);
    }

    @NonNull
    public static <K, V> CaffeineConcurrentMap<K, V> expireAfterAccess(@NonNull Duration duration) {
        Cache<K, V> builder = Caffeine.newBuilder().expireAfterAccess(duration).build();
        return new CaffeineConcurrentMap<>(builder);
    }

    private ConcurrentMap<K, V> map() {
        return delegate.asMap();
    }

    @Override
    public V put(K key, V value) {
        return map().put(key, value);
    }

    @Override
    public V putIfAbsent(@NonNull K key, V value) {
        return map().putIfAbsent(key, value);
    }

    @Override
    public boolean remove(@NonNull Object key, Object value) {
        return map().remove(key, value);
    }

    @Override
    public V remove(Object key) {
        return map().remove(key);
    }

    @Override
    public boolean replace(@NonNull K key, @NonNull V oldValue, @NonNull V newValue) {
        return map().replace(key, oldValue, newValue);
    }

    @Override
    public V replace(@NonNull K key, @NonNull V value) {
        return map().replace(key, value);
    }

    @Override
    @NonNull
    public Set<Map.Entry<K, V>> entrySet() {
        return map().entrySet();
    }

    @Override
    public int size() {
        return map().size();
    }

    @Override
    public void clear() {
        delegate.invalidateAll();
    }


}