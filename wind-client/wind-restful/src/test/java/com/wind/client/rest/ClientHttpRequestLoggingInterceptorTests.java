package com.wind.client.rest;

import com.wind.common.WindConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import javax.imageio.IIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author wuxp
 * @date 2026-02-10 16:20
 **/
class ClientHttpRequestLoggingInterceptorTests {

    private final ClientHttpRequestLoggingInterceptor interceptor = new ClientHttpRequestLoggingInterceptor(WindConstants.DEV);

    @Test
    void testLog() throws IOException {
        HttpRequest request = ApiSignatureRequestInterceptorTests.mockHttpRequest();
        ClientHttpRequestExecution execution = ApiSignatureRequestInterceptorTests.mockExecution();
        ClientHttpResponse response = interceptor.intercept(request, "mock body".getBytes(StandardCharsets.UTF_8), execution);
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}
