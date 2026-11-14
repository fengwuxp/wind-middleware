package com.wind.server.web.security;

import com.alibaba.fastjson2.JSON;
import com.wind.common.WindConstants;
import com.wind.common.util.StringJoinSplitUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;

/**
 * @author wuxp
 * @date 2024-03-16 10:11
 **/
class IpAccessControlFilterTests {

    @Test
    void testUnknownIpAllowed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        getFilter(WindConstants.EMPTY, WindConstants.EMPTY).doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testAllowed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        getFilter(WindConstants.EMPTY, WindConstants.EMPTY).doFilter(mockHttpRequest(), response, new MockFilterChain());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testConfigSupplierConstructorUsesNonStrictMode() throws Exception {
        IpAccessControlFilter filter = new IpAccessControlFilter(request ->
                getIpAccessControlConfig("10.0.0.0/8", WindConstants.EMPTY));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertSame(request, chain.getRequest());
    }

    /**
     * 同一过滤器按当前请求选择名单，验证动态配置不会固定为首个请求的结果。
     */
    @Test
    void testConfigSupplierSelectsRulesForEachRequest() throws Exception {
        IpAccessControlFilter filter = new IpAccessControlFilter(request -> {
            if ("/public".equals(request.getRequestURI())) {
                return getIpAccessControlConfig("192.168.0.0/16", WindConstants.EMPTY);
            }
            return getIpAccessControlConfig("10.0.0.0/8", WindConstants.EMPTY);
        });
        try {
            MockHttpServletRequest allowedRequest = mockHttpRequest();
            allowedRequest.setRequestURI("/public");
            MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
            MockFilterChain allowedChain = new MockFilterChain();
            filter.doFilter(allowedRequest, allowedResponse, allowedChain);

            Assertions.assertEquals(200, allowedResponse.getStatus());
            Assertions.assertSame(allowedRequest, allowedChain.getRequest());

            MockHttpServletRequest rejectedRequest = mockHttpRequest();
            rejectedRequest.setRequestURI("/internal");
            MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
            MockFilterChain rejectedChain = new MockFilterChain();
            filter.doFilter(rejectedRequest, rejectedResponse, rejectedChain);

            assertRejected(rejectedResponse);
            Assertions.assertNull(rejectedChain.getRequest());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testStrictModeAllowsValidIpWithoutAccessControlConfig() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        getFilter(WindConstants.EMPTY, WindConstants.EMPTY, true).doFilter(mockHttpRequest(), response, new MockFilterChain());

        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testStrictModeRejectsMissingIpEvenWithoutAccessControlConfig() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        getFilter(WindConstants.EMPTY, WindConstants.EMPTY, true).doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertRejected(response);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "not-an-ip", "192.168.0.999"})
    void testStrictModeRejectsInvalidIp(String clientIp) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();

        getFilter(WindConstants.EMPTY, WindConstants.EMPTY, true).doFilter(request, response, new MockFilterChain());

        assertRejected(response);
    }

    @Test
    void testStrictModeStillAppliesWhitelistAndBlacklist() throws Exception {
        MockHttpServletResponse whitelistResponse = new MockHttpServletResponse();
        MockHttpServletResponse blacklistResponse = new MockHttpServletResponse();

        getFilter("192.168.0.0/16", WindConstants.EMPTY, true).doFilter(mockHttpRequest(), whitelistResponse, new MockFilterChain());
        getFilter("192.168.0.0/16", "192.168.0.0/16", true).doFilter(mockHttpRequest(), blacklistResponse, new MockFilterChain());

        Assertions.assertEquals(200, whitelistResponse.getStatus());
        assertRejected(blacklistResponse);
    }

    @NonNull
    private static MockHttpServletRequest mockHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, "192.168.0.102");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    @Test
    void testMatchWhitelistAllowed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        getFilter("192.168.0.0/16", WindConstants.EMPTY).doFilter(mockHttpRequest(), response, new MockFilterChain());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testAllowMatches() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        getFilter("0.0.0.0/0", WindConstants.EMPTY).doFilter(mockHttpRequest(), response, new MockFilterChain());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testMatchBlacklistRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        getFilter("192.168.0.0/16", "192.168.0.0/16").doFilter(mockHttpRequest(), response, new MockFilterChain());
        Assertions.assertEquals(400, response.getStatus());
        String errorMessage = JSON.parseObject(response.getContentAsString()).getString("errorMessage");
        Assertions.assertEquals("client source ip not allow access", errorMessage);
    }

    private IpAccessControlFilter getFilter(String whitelist, String blacklist) {
        return getFilter(whitelist, blacklist, false);
    }

    private IpAccessControlFilter getFilter(String whitelist, String blacklist, boolean strict) {
        return new IpAccessControlFilter(getIpAccessControlConfig(whitelist, blacklist), strict);
    }

    private static IpAccessControlFilter.@NonNull IpAccessControlConfig getIpAccessControlConfig(String whitelist, String blacklist) {
        return new IpAccessControlFilter.IpAccessControlConfig(StringJoinSplitUtils.split(whitelist), StringJoinSplitUtils.split(blacklist));
    }

    private static void assertRejected(MockHttpServletResponse response) throws Exception {
        Assertions.assertEquals(400, response.getStatus());
        String errorMessage = JSON.parseObject(response.getContentAsString()).getString("errorMessage");
        Assertions.assertEquals("client source ip not allow access", errorMessage);
    }
}
