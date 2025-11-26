package com.wind.middleware.idempotent;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author wuxp
 * @date 2025-10-13 11:44
 **/
class WindIdempotentExecuteUtilsTests {

    private static final Map<String, Boolean> KEY_SATES = new LinkedHashMap<>();

    private static final Map<String, Object> STORAGE_CACHE = new LinkedHashMap<>();

    @BeforeEach
    void setup() {
        init();
    }

    @AfterEach
    void after() {
        clear();
    }

    @Test
    void testExecuteWithReturnValue() {
        String idempotentKey = "test";
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> 1);
        Assertions.assertEquals(1, STORAGE_CACHE.get(idempotentKey));
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> 2);
        Assertions.assertEquals(1, STORAGE_CACHE.get(idempotentKey));
    }

    @Test
    void testExecuteWithReturnVoid() {
        String idempotentKey = "test";
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> {
        });
        Assertions.assertTrue(KEY_SATES.get(idempotentKey));
        Assertions.assertNull(STORAGE_CACHE.get(idempotentKey));
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> {
        });
        Assertions.assertNull(STORAGE_CACHE.get(idempotentKey));
    }

    static void init() {
        WindIdempotentExecuteUtils.configureStorage(new WindIdempotentKeyStorage() {
            @Override
            public void save(String idempotentKey, Object value) {
                KEY_SATES.put(idempotentKey, true);
                STORAGE_CACHE.computeIfAbsent(idempotentKey, k -> value);
            }

            @Nullable
            @Override
            public WindIdempotentValueWrapper checkExistsAndGetValue(String idempotentKey) {
                if (exists(idempotentKey)) {
                    Object val = STORAGE_CACHE.get(idempotentKey);
                    return new KryoWindIdempotentValueWrapper(val);
                }
                return null;
            }

            @Override
            public boolean exists(String idempotentKey) {
                return Objects.equals(KEY_SATES.get(idempotentKey), true);
            }
        });
    }

    static void clear() {
        KEY_SATES.clear();
        STORAGE_CACHE.clear();
    }

}
