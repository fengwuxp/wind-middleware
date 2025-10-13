package com.wind.middleware.idempotent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wuxp
 * @date 2025-10-13 11:00
 **/
class KryoWindIdempotentValueWrapperTests {

    @Test
    void testWithJavaObject() {
        assertWithObject(123);
        assertWithObject(false);
        assertWithObject("123");
        assertWithObject(List.of(1, 2, 3));
        assertWithObject(Set.of("1", 2, 3));
        assertWithObject(Map.of("1", 2, "3", 4));
    }

    @Test
    void testWithArray() {
        KryoWindIdempotentValueWrapper wrapper = new KryoWindIdempotentValueWrapper(new String[]{"1", "2"});
        String[] v2 = KryoWindIdempotentValueWrapper.of(wrapper.asText()).getValue();
        Assertions.assertArrayEquals(wrapper.getValue(), v2);
    }

    @Test
    void testWithPojo() {
        assertWithObject(new TestObject<>("1", 2, false));
        assertWithObject(new TestObject<>("1", 2, List.of(1, 2, 3)));
    }

    private void assertWithObject(Object value) {
        KryoWindIdempotentValueWrapper wrapper = new KryoWindIdempotentValueWrapper(value);
        Object v2 = KryoWindIdempotentValueWrapper.of(wrapper.asText()).getValue();
        Assertions.assertEquals(value, v2);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestObject<T> {
        private String name;

        private int age;

        private T val;
    }

}
