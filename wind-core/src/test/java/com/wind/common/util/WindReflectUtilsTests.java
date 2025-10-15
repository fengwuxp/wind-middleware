package com.wind.common.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author wuxp
 * @date 2024-09-14 13:50
 **/
class WindReflectUtilsTests {


    @Test
    void testNewInstance() {
        Example example = WindReflectUtils.newInstance(Example.class, Map.of("name","zhangs"));
        Assertions.assertNotNull(example);
        Assertions.assertEquals("zhangs", example.getName());
    }

    @Test
    void testFindFieldGetMethod() {
        Field field = WindReflectUtils.findField(Example.class, "name");
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

    @Test
    void testGetFieldValue() {
        Example target = new Example();
        target.setName("test");
        String name = WindReflectUtils.getFieldValue("name", target);
        Assertions.assertEquals(target.getName(), name);
    }

    @Test
    void testSetFieldValue() {
        Example target = new Example();
        WindReflectUtils.setFieldValue("name", target, "test");
        Assertions.assertEquals("test", target.getName());
    }

    @Test
    void testFindFields() {
        List<String> fieldNames = WindReflectUtils.getFieldNames(Example.class);
        Field[] fields = WindReflectUtils.findFields(Example.class, fieldNames);
        boolean match = Arrays.stream(fields).map(Field::accessFlags)
                .flatMap(Collection::stream)
                .anyMatch(accessFlag -> accessFlag == AccessFlag.PRIVATE);
        Assertions.assertTrue(match);
    }

    @Test
    void testGetterMethods() {
        Method[] getterMethods = WindReflectUtils.getGetterMethods(Example.class);
        Assertions.assertNotNull(getterMethods);
        Assertions.assertEquals(1, getterMethods.length);
    }

    @Test
    void testExchangeGetterHandle() throws Throwable {
        List<String> fieldNames = WindReflectUtils.getFieldNames(Example.class);
        Field[] fields = WindReflectUtils.findFields(Example.class, fieldNames);
        MethodHandle getterHandle = WindReflectUtils.exchangeGetterHandle(fields[0]);
        Assertions.assertNotNull(getterHandle);
        Assertions.assertEquals(fields[0].getType(), getterHandle.type().returnType());
        Example example = new Example();
        example.setName("1");
        Object result = getterHandle.invoke(example);
        Assertions.assertEquals(example.name, result);
    }

    @Test
    void testExchangeSetterHandle() throws Throwable {
        List<String> fieldNames = WindReflectUtils.getFieldNames(Example.class);
        Field[] fields = WindReflectUtils.findFields(Example.class, fieldNames);
        MethodHandle setterHandle = WindReflectUtils.exchangeSetterHandle(fields[0]);
        Assertions.assertNotNull(setterHandle);
        Example example = new Example();
        setterHandle.invoke(example, "11");
        Assertions.assertEquals("11", example.name);
    }

    @Test
    void testExchangeMethodHandle() throws Throwable {
        Method[] getterMethods = WindReflectUtils.getGetterMethods(Example.class);
        Assertions.assertNotNull(getterMethods);
        MethodHandle methodHandle = WindReflectUtils.exchangeMethodHandle(getterMethods[0]);
        Assertions.assertNotNull(methodHandle);
        Example example = new Example();
        example.setName("1");
        Object result = methodHandle.invoke(example);
        Assertions.assertEquals(example.name, result);
    }

    interface A<E> {

        void say(E e);
    }

    abstract static class AbstractDemo<T, R> implements Function<T, R> {
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class Example extends AbstractDemo<String, List<String>> implements A<String> {

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
