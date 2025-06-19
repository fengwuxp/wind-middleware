package com.wind.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author wuxp
 * @date 2024-09-14 13:50
 **/
class WindReflectUtilsTests {


    @Test
    void testResolveSuperGenericTypeV1() {
        Type[] types = WindReflectUtils.resolveSuperGenericType(new Example());
        Assertions.assertEquals(2, types.length);
    }

    @Test
    void testResolveSuperGenericTypeV2() {
        Type[] types = WindReflectUtils.resolveSuperGenericType(new Example2());
        Assertions.assertEquals(2, types.length);
    }

    static class Example implements Function<String, List<String>> {

        @Override
        public List<String> apply(String s) {
            return Collections.emptyList();
        }
    }

    static abstract class AbstractDemo<T, R> implements Function<T, R> {
    }

    static class Example2 extends AbstractDemo<String, List<String>> {

        @Override
        public List<String> apply(String s) {
            return Collections.emptyList();
        }
    }
}
