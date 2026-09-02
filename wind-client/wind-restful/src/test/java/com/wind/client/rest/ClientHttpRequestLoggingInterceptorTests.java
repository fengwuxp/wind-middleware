package com.wind.client.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wuxp
 * @date 2026-02-10 16:20
 **/
class ClientHttpRequestLoggingInterceptorTests {

    private final ClientHttpRequestLoggingInterceptor interceptor = new ClientHttpRequestLoggingInterceptor(WindConstants.DEV);

    @Test
    void testDefaultSuccessfulLogOmitsBodies() throws IOException {
        ClientHttpRequestLoggingInterceptor defaultInterceptor = new ClientHttpRequestLoggingInterceptor();
        HttpRequest request = ApiSignatureRequestInterceptorTests.mockHttpRequest();
        ClientHttpRequestExecution execution = ApiSignatureRequestInterceptorTests.mockExecution();
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            ClientHttpResponse response = defaultInterceptor.intercept(request, "mock body".getBytes(StandardCharsets.UTF_8), execution);
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> Assertions.assertEquals(Level.INFO, appender.list.getFirst().getLevel()),
                    () -> Assertions.assertTrue(message.contains("RequestBody: -")),
                    () -> Assertions.assertTrue(message.contains("ResponseBody: -")),
                    () -> Assertions.assertFalse(message.contains("mock body"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testProdSelectsLogLevelByResponseStatus() throws IOException {
        ClientHttpRequestLoggingInterceptor prodInterceptor = new ClientHttpRequestLoggingInterceptor(WindConstants.PROD);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            prodInterceptor.intercept(request("/api/v1/user", headers), "{}".getBytes(StandardCharsets.UTF_8),
                    execution("{}", MediaType.APPLICATION_JSON, HttpStatus.OK));
            prodInterceptor.intercept(request("/api/v1/user", headers), "{}".getBytes(StandardCharsets.UTF_8),
                    execution("{}", MediaType.APPLICATION_JSON, HttpStatus.BAD_REQUEST));
            Assertions.assertEquals(List.of(Level.DEBUG, Level.ERROR),
                    appender.list.stream().map(ILoggingEvent::getLevel).toList());
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testExplicitSuccessfulLogMasksBodies() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            WindTracer.TRACER.call(() -> {
                WindTracer.TRACER.putVariable(ClientHttpRequestLoggingInterceptor.ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME, true);
                return interceptor.intercept(request("/api/v1/user", headers),
                        "{\"password\":\"request-success-secret\"}".getBytes(StandardCharsets.UTF_8),
                        execution("{\"token\":\"response-success-secret\"}", MediaType.APPLICATION_JSON, HttpStatus.OK));
            });
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("\"password\":\"******\"")),
                    () -> Assertions.assertTrue(message.contains("\"token\":\"******\"")),
                    () -> Assertions.assertFalse(message.contains("request-success-secret")),
                    () -> Assertions.assertFalse(message.contains("response-success-secret"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testDefaultLogOmitsSensitiveData() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json; charset=utf-8"));
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer request-credential");
        headers.add(HttpHeaders.COOKIE, "session=request-cookie");
        headers.add("Wind-Sign", "request-signature");
        headers.add("X-ApiKey", "request-api-key");
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            interceptor.intercept(request("/api/v1/user?access_token=query-credential", headers),
                    "{\"password\":\"request-body-secret\",\"name\":\"request-visible\"}".getBytes(StandardCharsets.UTF_8),
                    execution("{\"data\":[{\"token\":\"response-body-secret\",\"message\":\"response-visible\"}]}"));
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("URI: /api/v1/user")),
                    () -> Assertions.assertTrue(message.contains("[****]")),
                    () -> Assertions.assertFalse(message.contains("RequestBody: -")),
                    () -> Assertions.assertFalse(message.contains("ResponseBody: -")),
                    () -> Assertions.assertFalse(message.contains("query-credential")),
                    () -> Assertions.assertFalse(message.contains("request-credential")),
                    () -> Assertions.assertFalse(message.contains("request-cookie")),
                    () -> Assertions.assertFalse(message.contains("request-signature")),
                    () -> Assertions.assertFalse(message.contains("request-api-key")),
                    () -> Assertions.assertFalse(message.contains("request-body-secret")),
                    () -> Assertions.assertFalse(message.contains("response-body-secret")),
                    () -> Assertions.assertTrue(message.contains("\"password\":\"******\"")),
                    () -> Assertions.assertTrue(message.contains("\"token\":\"******\"")),
                    () -> Assertions.assertTrue(message.contains("request-visible")),
                    () -> Assertions.assertTrue(message.contains("response-visible")),
                    () -> Assertions.assertFalse(message.contains("response-cookie"))
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testJsonBodyMasksCommonCredentialFieldVariants() throws IOException {
        String requestBody = """
                {"authorization":"body-secret-1","credentials":"body-secret-2","accessToken":"body-secret-3",
                 "client_secret":"body-secret-4","api-key":"body-secret-5","nested":{"username":"body-secret-6",
                 "passwd":"body-secret-7"},"message":"body-visible"}
                """;
        String message = interceptAndGetLog(MediaType.APPLICATION_JSON, requestBody,
                "{\"access_id\":\"body-secret-8\",\"nonce\":\"body-secret-9\"}", MediaType.APPLICATION_JSON);
        Assertions.assertAll(
                () -> Assertions.assertFalse(message.contains("body-secret-")),
                () -> Assertions.assertTrue(message.contains("body-visible")),
                () -> Assertions.assertTrue(message.contains("\"accessToken\":\"******\"")),
                () -> Assertions.assertTrue(message.contains("\"username\":\"******\"")),
                () -> Assertions.assertTrue(message.contains("\"access_id\":\"******\""))
        );
    }

    @Test
    void testExplicitOversizedBodyLogIsRedacted() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String requestBody = "{\"password\":\"request-head-secret\",\"payload\":\"" + "r".repeat(2500) + "request-tail-secret\"}";
        String responseBody = "{\"token\":\"response-head-secret\",\"payload\":\"" + "s".repeat(2500) + "response-tail-secret\"}";
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            WindTracer.TRACER.call(() -> {
                WindTracer.TRACER.putVariable(ClientHttpRequestLoggingInterceptor.ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME, true);
                return interceptor.intercept(request("/api/v1/user", headers), requestBody.getBytes(StandardCharsets.UTF_8), execution(responseBody));
            });
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("RequestBody: [****]")),
                    () -> Assertions.assertTrue(message.contains("ResponseBody: [****]")),
                    () -> Assertions.assertFalse(message.contains("request-head-secret")),
                    () -> Assertions.assertFalse(message.contains("response-head-secret")),
                    () -> Assertions.assertFalse(message.contains("request-tail-secret")),
                    () -> Assertions.assertFalse(message.contains("response-tail-secret"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testBodyAtMaximumLengthIsStillStructurallyMasked() throws IOException {
        String prefix = "{\"password\":\"boundary-secret\",\"payload\":\"";
        String suffix = "\"}";
        int payloadLength = 2048 - prefix.getBytes(StandardCharsets.UTF_8).length - suffix.getBytes(StandardCharsets.UTF_8).length;
        String body = prefix + "x".repeat(payloadLength) + suffix;
        Assertions.assertEquals(2048, body.getBytes(StandardCharsets.UTF_8).length);

        String message = interceptAndGetLog(MediaType.APPLICATION_JSON, body, body, MediaType.APPLICATION_JSON);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains("\"password\":\"******\"")),
                () -> Assertions.assertTrue(message.contains("\"payload\":\"xxx")),
                () -> Assertions.assertFalse(message.contains("boundary-secret")),
                () -> Assertions.assertFalse(message.contains("RequestBody: [****]")),
                () -> Assertions.assertFalse(message.contains("ResponseBody: [****]"))
        );
    }

    @Test
    void testUnmaskableBodiesAreRedacted() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            interceptor.intercept(request("/api/v1/user", headers),
                    "invalid-json-request-secret".getBytes(StandardCharsets.UTF_8),
                    execution("plain-response-secret", MediaType.TEXT_PLAIN));
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains("RequestBody: [****]")),
                    () -> Assertions.assertTrue(message.contains("ResponseBody: [****]")),
                    () -> Assertions.assertFalse(message.contains("invalid-json-request-secret")),
                    () -> Assertions.assertFalse(message.contains("plain-response-secret"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/xml", "text/xml", "application/atom+xml", "text/html", "text/plain"})
    void testUnstructuredTextBodyIsRedacted(String contentTypeValue) throws IOException {
        MediaType contentType = MediaType.parseMediaType(contentTypeValue);
        String message = interceptAndGetLog(contentType, "request-text-secret", "response-text-secret", contentType);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains("RequestBody: [****]")),
                () -> Assertions.assertTrue(message.contains("ResponseBody: [****]")),
                () -> Assertions.assertFalse(message.contains("request-text-secret")),
                () -> Assertions.assertFalse(message.contains("response-text-secret"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"multipart/form-data;boundary=test", "application/octet-stream", "image/png", "text/csv"})
    void testUnsupportedContentTypeOmitsBodies(String contentTypeValue) throws IOException {
        MediaType contentType = MediaType.parseMediaType(contentTypeValue);
        String message = interceptAndGetLog(contentType, "request-binary-secret", "response-binary-secret", contentType);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains("RequestBody: -")),
                () -> Assertions.assertTrue(message.contains("ResponseBody: -")),
                () -> Assertions.assertFalse(message.contains("request-binary-secret")),
                () -> Assertions.assertFalse(message.contains("response-binary-secret"))
        );
    }

    @Test
    void testMissingContentTypeOmitsBodies() throws IOException {
        String message = interceptAndGetLog(null, "request-missing-type-secret", "response-missing-type-secret", null);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains("RequestBody: -")),
                () -> Assertions.assertTrue(message.contains("ResponseBody: -")),
                () -> Assertions.assertFalse(message.contains("request-missing-type-secret")),
                () -> Assertions.assertFalse(message.contains("response-missing-type-secret"))
        );
    }

    @Test
    void testRequestAndResponseUseTheirOwnContentTypes() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MediaType responseContentType = MediaType.parseMediaType("application/vnd.wind+json");
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            interceptor.intercept(request("/api/v1/token", headers),
                    "username=request-user&pass%77ord=request-secret&design=request-visible&scope=profile".getBytes(StandardCharsets.UTF_8),
                    execution("{\"token\":\"response-secret\",\"message\":\"response-visible\"}", responseContentType));
            String message = logged(appender);
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.contains(
                            "RequestBody: username=[****]&pass%77ord=[****]&design=request-visible&scope=profile")),
                    () -> Assertions.assertTrue(message.contains("\"token\":\"******\"")),
                    () -> Assertions.assertTrue(message.contains("request-visible")),
                    () -> Assertions.assertTrue(message.contains("response-visible")),
                    () -> Assertions.assertFalse(message.contains("request-user")),
                    () -> Assertions.assertFalse(message.contains("request-secret")),
                    () -> Assertions.assertFalse(message.contains("response-secret"))
            );
        } finally {
            stopLogCapture(appender);
        }
    }

    @Test
    void testFormBodyMasksRepeatedEmptyAndCaseInsensitiveSensitiveParameters() throws IOException {
        MediaType contentType = MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8");
        String message = interceptAndGetLog(contentType,
                "scope=read&scope=write&password=&token=form-secret&PASSWORD=upper-secret",
                "result=visible", contentType);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains(
                        "RequestBody: scope=read&scope=write&password=[****]&token=[****]&PASSWORD=[****]")),
                () -> Assertions.assertTrue(message.contains("ResponseBody: result=visible")),
                () -> Assertions.assertFalse(message.contains("form-secret")),
                () -> Assertions.assertFalse(message.contains("upper-secret"))
        );
    }

    @Test
    void testMalformedFormBodyIsRedacted() throws IOException {
        String message = interceptAndGetLog(MediaType.APPLICATION_FORM_URLENCODED,
                "pass%ZZword=form-secret&scope=profile", "result=visible", MediaType.APPLICATION_FORM_URLENCODED);
        Assertions.assertAll(
                () -> Assertions.assertTrue(message.contains("RequestBody: [****]")),
                () -> Assertions.assertTrue(message.contains("ResponseBody: result=visible")),
                () -> Assertions.assertFalse(message.contains("form-secret"))
        );
    }

    private String interceptAndGetLog(MediaType requestContentType, String requestBody, String responseBody,
                                      MediaType responseContentType) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        if (requestContentType != null) {
            headers.setContentType(requestContentType);
        }
        ListAppender<ILoggingEvent> appender = startLogCapture();
        try {
            interceptor.intercept(request("/api/v1/user", headers), requestBody.getBytes(StandardCharsets.UTF_8),
                    execution(responseBody, responseContentType));
            return logged(appender);
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
        return execution(responseBody, MediaType.APPLICATION_JSON);
    }

    private static ClientHttpRequestExecution execution(String responseBody, MediaType contentType) {
        return execution(responseBody, contentType, HttpStatus.BAD_REQUEST);
    }

    private static ClientHttpRequestExecution execution(String responseBody, MediaType contentType, HttpStatus status) {
        return (request, body) -> new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return status;
            }

            @Override
            public String getStatusText() {
                return status.getReasonPhrase();
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
                if (contentType != null) {
                    headers.setContentType(contentType);
                }
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
