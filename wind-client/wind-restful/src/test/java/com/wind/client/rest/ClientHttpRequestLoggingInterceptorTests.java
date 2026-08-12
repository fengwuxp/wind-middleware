package com.wind.client.rest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

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

    @Test
    void testDefaultLogOmitsSensitiveData() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer request-credential");
        headers.add(HttpHeaders.COOKIE, "session=request-cookie");
        headers.add("Wind-Sign", "request-signature");
        headers.add("X-ApiKey", "request-api-key");
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            interceptor.intercept(request("/api/v1/user?access_token=query-credential", headers),
                    "{\"password\":\"request-body-secret\"}".getBytes(StandardCharsets.UTF_8),
                    execution("{\"token\":\"response-body-secret\"}"));
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("URI: /api/v1/user")),
                    () -> Assertions.assertTrue(message.contains("[****]")),
                    () -> Assertions.assertTrue(message.contains("RequestBody: -")),
                    () -> Assertions.assertTrue(message.contains("ResponseBody: -")),
                    () -> Assertions.assertFalse(message.contains("query-credential")),
                    () -> Assertions.assertFalse(message.contains("request-credential")),
                    () -> Assertions.assertFalse(message.contains("request-cookie")),
                    () -> Assertions.assertFalse(message.contains("request-signature")),
                    () -> Assertions.assertFalse(message.contains("request-api-key")),
                    () -> Assertions.assertFalse(message.contains("request-body-secret")),
                    () -> Assertions.assertFalse(message.contains("response-body-secret")),
                    () -> Assertions.assertFalse(message.contains("response-cookie"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testExplicitBodyLogIsLimited() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String requestBody = "{\"payload\":\"" + "r".repeat(2500) + "request-tail-secret\"}";
        String responseBody = "{\"payload\":\"" + "s".repeat(2500) + "response-tail-secret\"}";
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            WindTracer.TRACER.call(() -> {
                WindTracer.TRACER.putVariable(ClientHttpRequestLoggingInterceptor.ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME, true);
                return interceptor.intercept(request("/api/v1/user", headers), requestBody.getBytes(StandardCharsets.UTF_8), execution(responseBody));
            });
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("{\"payload\":\"rrr")),
                    () -> Assertions.assertTrue(message.contains("...[truncated")),
                    () -> Assertions.assertFalse(message.contains("request-tail-secret")),
                    () -> Assertions.assertFalse(message.contains("response-tail-secret"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    private static HttpRequest request(String uri, HttpHeaders headers) {
        return new HttpRequest() {
            @Override
            public HttpMethod getMethod() {
                return HttpMethod.POST;
            }

            @Override
            public URI getURI() {
                return URI.create(uri);
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }
        };
    }

    private static ClientHttpRequestExecution execution(String responseBody) {
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
            public InputStream getBody() {
                return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                headers.add(HttpHeaders.SET_COOKIE, "session=response-cookie");
                return headers;
            }
        };
    }

    private static ListAppender<ILoggingEvent> startLogCapture() {
        ListAppender<ILoggingEvent> result = new ListAppender<>();
        result.start();
        ((Logger) LoggerFactory.getLogger(ClientHttpRequestLoggingInterceptor.class)).addAppender(result);
        return result;
    }

    private static void stopLogCapture(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(ClientHttpRequestLoggingInterceptor.class)).detachAppender(appender);
        appender.stop();
    }

    private static String logged(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
    }
}
