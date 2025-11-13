package com.wind.server.web.security;

import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.client.rest.ApiSignatureRequestInterceptor;
import com.wind.common.WindConstants;
import com.wind.common.WindHttpConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.wind.server.web.security.RequestSignFilter.SIGNATURE_TIMESTAMP_VALIDITY_PERIOD;

/**
 * @author wuxp
 * @date 2024-03-04 13:14
 **/
class RequestSignFilterTests {

    private final ApiSecretAccount secretAccount = ApiSecretAccount.hmacSha256(RandomStringUtils.secure().nextAlphabetic(12),
            RandomStringUtils.secure().nextAlphabetic(32));

    private RequestSignFilter signFilter;

    @BeforeEach
    void setup() {
        System.setProperty(WindConstants.SPRING_PROFILES_ACTIVE, WindConstants.DEV);
        signFilter = new RequestSignFilter((accessId, secretVersion) -> secretAccount, Collections.emptyList());
    }

    @Test
    void testSignError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/examples");
        MockHttpServletResponse response = new MockHttpServletResponse();
        signFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void testSignSuccess() throws Exception {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://www.example.com/api/v1/examples?a=2&b=20&name=张三").build();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uriComponents.getPath());
        uriComponents.getQueryParams().forEach((name, values) -> {
            if (!ObjectUtils.isEmpty(values)) {
                request.setParameter(name, values.getFirst());
            }
        });
        byte[] requestBody = RandomStringUtils.secure().nextAlphabetic(1000).getBytes(StandardCharsets.UTF_8);
        request.setContent(requestBody);
        ApiSignatureRequestInterceptor interceptor = new ApiSignatureRequestInterceptor(httpRequest -> secretAccount);
        interceptor.intercept(new ServletServerHttpRequest(request), requestBody, (r, body) -> {
            r.getHeaders().forEach((name, values) -> {
                if (!ObjectUtils.isEmpty(values)) {
                    request.addHeader(name, values.getFirst());
                }
            });
            return new MockClientHttpResponse(new byte[0], 200);
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        signFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertNotNull(request.getAttribute(WindHttpConstants.API_SECRET_ACCOUNT_ATTRIBUTE_NAME));
    }

    @Test
    void testSignExpire() throws Exception {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString("https://www.example.com/api/v1/examples?a=2&b=20&name=张三").build();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uriComponents.getPath());
        byte[] requestBody = RandomStringUtils.secure().nextAlphabetic(1000).getBytes(StandardCharsets.UTF_8);
        request.setContent(requestBody);
        ApiSignatureRequestInterceptor interceptor = new ApiSignatureRequestInterceptor(httpRequest -> secretAccount);
        interceptor.intercept(new ServletServerHttpRequest(request), requestBody, (r, body) -> {
            r.getHeaders().forEach((name, values) -> {
                if (name.contains("Timestamp")) {
                    request.addHeader(name, System.currentTimeMillis() - SIGNATURE_TIMESTAMP_VALIDITY_PERIOD.get() - 1);
                } else {
                    if (!ObjectUtils.isEmpty(values)) {
                        request.addHeader(name, values.getFirst());
                    }
                }
            });
            return new MockClientHttpResponse(new byte[0], 200);
        });
        MockHttpServletResponse response = new MockHttpServletResponse();
        signFilter.doFilter(request, response, new MockFilterChain());
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("sign verify error"));
    }
}
