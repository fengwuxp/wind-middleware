package com.wind.script.spring;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author wuxp
 * @date 2024-09-12 13:23
 **/
class SpringExpressionOperatorsTests {

    @Test
    void testContainsText() {
        Assertions.assertTrue(SpringExpressionOperators.contains("zhans", "an"));
        Assertions.assertFalse(SpringExpressionOperators.contains("an", "zhans"));
    }

    @Test
    void testContainsCollection() {
        Assertions.assertTrue(SpringExpressionOperators.contains(ImmutableSet.of("a", "b", "c"), "c"));
        Assertions.assertTrue(SpringExpressionOperators.contains("c", List.of("a", "b", "c")));
        Assertions.assertTrue(SpringExpressionOperators.contains(1, List.of(1, 2, 3)));
    }

    @Test
    void testContainsObjectArray() {
        Assertions.assertTrue(SpringExpressionOperators.contains(new String[]{"ab", "c"}, "c"));
        Assertions.assertTrue(SpringExpressionOperators.contains(1L, new Long[]{1L, 2L, 3L}));
        Assertions.assertFalse(SpringExpressionOperators.contains(1, new Long[]{1L, 2L, 3L}));
    }

    @Test
    void testContainsPrimitiveArray() {
        Assertions.assertTrue(SpringExpressionOperators.contains(new char[]{'a', 'b', 'c'}, 'b'));
        Assertions.assertTrue(SpringExpressionOperators.contains(new byte[]{1, 2, 3, 4}, Byte.valueOf("1")));
        Assertions.assertTrue(SpringExpressionOperators.contains(new short[]{1, 2, 3, 4}, Short.valueOf("1")));
        Assertions.assertTrue(SpringExpressionOperators.contains(new int[]{1, 2, 3, 4}, 1));
        Assertions.assertTrue(SpringExpressionOperators.contains(new long[]{1, 2, 3, 4}, 1L));
        Assertions.assertTrue(SpringExpressionOperators.contains(1d, new double[]{1, 2, 3, 4}));
        Assertions.assertTrue(SpringExpressionOperators.contains(1f, new float[]{1, 2, 3, 4}));
        Assertions.assertTrue(SpringExpressionOperators.contains(false, new boolean[]{false, true}));
    }

    @Test
    void testInRange() {
        Assertions.assertTrue(SpringExpressionOperators.inRange(1, List.of(1, 2)));
        Assertions.assertTrue(SpringExpressionOperators.inRange(1, new int[]{0, 3}));
        Assertions.assertFalse(SpringExpressionOperators.inRange(10, new int[]{0, 3}));
    }
}
