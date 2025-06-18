package com.wind.client.rest;

import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.api.core.signature.SignatureHttpHeaderNames;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author wuxp
 * @date 2024-02-21 17:30
 **/
class ApiSignatureRequestInterceptorTests {

    ApiSecretAccount secretAccount = ApiSecretAccount.hmacSha256(RandomStringUtils.randomAlphabetic(12), RandomStringUtils.randomAlphabetic(32));

    @Test
    void testSha256() throws IOException {
        ApiSignatureRequestInterceptor interceptor = new ApiSignatureRequestInterceptor(httpRequest -> secretAccount);
        ClientHttpResponse response = interceptor.intercept(mockHttpRequest(), new byte[0], mockExecution());
        HttpHeaders headers = response.getHeaders();
        String sign = new SignatureHttpHeaderNames(null).getSign();
        Assertions.assertNotNull(headers.get(sign));
    }

    private static ClientHttpRequestExecution mockExecution() {
        return (request, body) -> new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public String getStatusText() {
                return HttpStatus.OK.getReasonPhrase();
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream("success".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return request.getHeaders();
            }
        };
    }

    private HttpRequest mockHttpRequest() {
        HttpHeaders httpHeaders = new HttpHeaders();
        return new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.GET;
            }

            @Override
            public URI getURI() {
                return URI.create("/api/v1/user/1");
            }

            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }
        };
    }

}
