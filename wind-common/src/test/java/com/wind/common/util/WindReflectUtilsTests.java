package com.wind.common.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
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
        Assertions.assertEquals(2, WindReflectUtils.resolveSuperGenericType(new Example2()).length);
        Type[] types = WindReflectUtils.resolveSuperGenericType(new Example2(), A.class);
        Assertions.assertEquals(1, types.length);
        Assertions.assertEquals(String.class, types[0]);
    }

    @Test
    void testFindFieldGetMethod() {
        Field field = WindReflectUtils.findField(Example2.class, "name");
        Assertions.assertNotNull(field);
        Method method = WindReflectUtils.findFieldGetMethod(field);
        Assertions.assertNotNull(method);
    }

    @Test
    void testReflectIgnoreJavaModel() {
        List<String> fieldNames = WindReflectUtils.getFieldNames(BigDecimal.class);
        Field[] fields = WindReflectUtils.findFields(BigDecimal.class, fieldNames);
        boolean match = Arrays.stream(fields).map(Field::accessFlags)
                .flatMap(Collection::stream)
                .anyMatch(accessFlag -> accessFlag == AccessFlag.PRIVATE);
        Assertions.assertTrue(match);
    }

    interface A<E> {

        void say(E e);
    }

    static class Example implements Function<String, List<String>> {

        @Override
        public List<String> apply(String s) {
            return Collections.emptyList();
        }
    }

    abstract static class AbstractDemo<T, R> implements Function<T, R> {
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class Example2 extends AbstractDemo<String, List<String>> implements A<String> {

        private String name;

        @Override
        public List<String> apply(String s) {
            return Collections.emptyList();
        }

        @Override
        public void say(String s) {

        }
    }

}
