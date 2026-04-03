package com.wind.common.util;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import tools.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * @author wuxp
 * @date 2026-04-03 11:20
 **/
class WindContextVariablesUtilsTests {


    @Test
    void testAsVariable() {
        String username = "test";
        Map<String, Object> user = Map.of("username", username, "age", 1);
        Map<String, Object> context = Map.of("user", user);
        User example = WindContextVariablesUtils.asVariable(context, "user", new ParameterizedTypeReference<>() {
        });
        assert example != null;
        Assertions.assertEquals(username, example.getUsername());
        example = WindContextVariablesUtils.asVariable(context, "user", new TypeReference<>() {
        });
        Assertions.assertEquals(username, example.getUsername());
    }

    @Data
    static class User {

        private String username;

        private Integer age;
    }
}
