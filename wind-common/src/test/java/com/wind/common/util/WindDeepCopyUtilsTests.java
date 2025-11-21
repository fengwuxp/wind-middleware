package com.wind.common.util;

import com.esotericsoftware.kryo.KryoException;
import com.google.common.collect.ImmutableMap;
import com.wind.common.exception.BaseException;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author wuxp
 * @date 2024-08-07 14:45
 **/
class WindDeepCopyUtilsTests {

    @Test
    void testSimpleType() {
        Integer result = WindDeepCopyUtils.copy(1);
        Assertions.assertEquals(1, result);
    }

    @Test
    void testDeepCopyList() {
        List<Integer> result = WindDeepCopyUtils.copy(Arrays.asList(1, 2, 3));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());
    }

    @Test
    void testDeepCopySet() {
        Set<Integer> result = WindDeepCopyUtils.copy(Collections.singleton(13));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }

    @Test
    void testDeepCopyImmutableMapWitException() {
        BaseException exception = Assertions.assertThrows(BaseException.class, () -> WindDeepCopyUtils.copy(ImmutableMap.of("a", "2")));
        Assertions.assertEquals("kryo decode exception", exception.getMessage());
    }

    @Test
    void testDeepCopyExample() {
        DeepCopyExample example = new DeepCopyExample();
        example.setId(1L);
        example.setUserName("example");
        example.setAge(22);
        example.setTags(Arrays.asList("test", "t2"));
        DeepCopyExample result = WindDeepCopyUtils.copy(example);
        Assertions.assertEquals(result, example);
    }


    @Data
    public static class DeepCopyExample {

        private String userName;

        private boolean success;

        private Integer age;

        private long id;

        private List<String> tags;
    }
}
