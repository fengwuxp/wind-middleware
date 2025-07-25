package com.wind.security.web.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author wuxp
 * @date 2025-07-25 17:09
 **/

class RequestMatcherUtilsTests {

    @Test
    void testConvertPathMatchers_MatchByMethodAndPath() {
        Set<String> patterns = Set.of("GET /api/users/**", "POST /api/orders/**");
        Set<RequestMatcher> matchers = RequestMatcherUtils.convertPathMatchers(patterns);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/123");

        assertTrue(RequestMatcherUtils.matches(matchers, request));
    }

    @Test
    void testConvertPathMatchers_MatchByPathOnly() {
        Set<String> patterns = Set.of("/public/**");
        Set<RequestMatcher> matchers = RequestMatcherUtils.convertPathMatchers(patterns);

        MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/public/info");

        assertTrue(RequestMatcherUtils.matches(matchers, request));
    }

    @Test
    void testNoMatch() {
        Set<String> patterns = Set.of("POST /secure/**");
        Set<RequestMatcher> matchers = RequestMatcherUtils.convertPathMatchers(patterns);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure/data");

        assertFalse(RequestMatcherUtils.matches(matchers, request));
    }
}
