package com.wind.server.web.security;

import com.alibaba.fastjson2.JSON;
import com.wind.common.WindConstants;
import com.wind.common.util.StringJoinSplitUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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

    @NonNull
    private static MockHttpServletRequest mockHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, "192.168.0.102");
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
        return new IpAccessControlFilter(getIpAccessControlConfig(whitelist, blacklist));
    }

    private static IpAccessControlFilter.@NonNull IpAccessControlConfig getIpAccessControlConfig(String whitelist, String blacklist) {
        return new IpAccessControlFilter.IpAccessControlConfig(StringJoinSplitUtils.split(whitelist), StringJoinSplitUtils.split(blacklist));
    }
}
