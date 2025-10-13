package com.wind.middleware.idempotent;

import jakarta.annotation.Nullable;
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

    private final Map<String, Boolean> keySates = new LinkedHashMap<>();

    private final Map<String, Object> storage = new LinkedHashMap<>();

    @BeforeEach
    void setup() {
        WindIdempotentExecuteUtils.setStorage(new WindIdempotentKeyStorage() {
            @Override
            public void save(String idempotentKey, Object value) {
                keySates.put(idempotentKey, true);
                storage.computeIfAbsent(idempotentKey, k -> value);
            }

            @Nullable
            @Override
            public WindIdempotentValueWrapper checkExistsAndGetValue(String idempotentKey) {
                if (exists(idempotentKey)) {
                    Object val = storage.get(idempotentKey);
                    return new KryoWindIdempotentValueWrapper(val);
                }
                return null;
            }

            @Override
            public boolean exists(String idempotentKey) {
                return Objects.equals(keySates.get(idempotentKey), true);
            }
        });
    }

    @Test
    void testExecuteWithReturnValue() {
        String idempotentKey = "test";
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> 1);
        Assertions.assertEquals(1, storage.get(idempotentKey));
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> 2);
        Assertions.assertEquals(1, storage.get(idempotentKey));
    }

    @Test
    void testExecuteWithReturnVoid() {
        String idempotentKey = "test";
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> {});
        Assertions.assertTrue(keySates.get(idempotentKey));
        Assertions.assertNull(storage.get(idempotentKey));
        WindIdempotentExecuteUtils.execute(idempotentKey, () -> {});
        Assertions.assertNull(storage.get(idempotentKey));
    }
}
