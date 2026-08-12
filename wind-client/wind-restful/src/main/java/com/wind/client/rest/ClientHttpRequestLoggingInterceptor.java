package com.wind.client.rest;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 统一打印请求日志。显式开启正文日志时，需要使用 {@link org.springframework.http.client.BufferingClientHttpRequestFactory}
 * 包装 {@link org.springframework.http.client.ClientHttpRequestFactory}，保证响应体可以多次读取。
 *
 * @author wuxp
 * @date 2026-01-22 14:15
 */
@Slf4j
public class ClientHttpRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 在当前请求上下文中显式开启请求体和响应体日志的变量名
     */
    public static final String ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME = "api.request.log.print.enable";

    /**
     * 支持按 UTF-8 文本记录正文的媒体类型
     */
    private static final List<MediaType> LOGGABLE_BODY_MEDIA_TYPES = List.of(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.TEXT_HTML,
            MediaType.TEXT_PLAIN,
            MediaType.TEXT_XML,
            MediaType.APPLICATION_ATOM_XML
    );

    /**
     * 请求体和响应体日志允许记录的最大字节数
     */
    private static final int MAX_BODY_LENGTH = 1024 * 2;

    /**
     * 敏感请求头和响应头的脱敏占位内容
     */
    private static final String MASK_TEXT = "[****]";

    /**
     * 用于识别敏感请求头和响应头的名称片段
     */
    private static final List<String> SENSITIVE_HEADER_PARTS = List.of(
            "authorization", "authentication", "auth", "cookie", "token", "secret", "credential",
            "api-key", "apikey", "access-key", "accesskey", "access-id", "accessid", "signature", "sign", "nonce"
    );

    private final String env;

    public ClientHttpRequestLoggingInterceptor(String env) {
        this.env = env;
    }

    public ClientHttpRequestLoggingInterceptor() {
        this(WindConstants.UNKNOWN);
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        return WindTracer.TRACER.call(() -> {
            StopWatch watch = new StopWatch();
            watch.start();
            ClientHttpResponse response = execution.execute(request, body);
            watch.stop();
            log(request, body, response, watch.getTotalTimeMillis());
            return response;
        });
    }

    private void log(HttpRequest request, byte[] body, ClientHttpResponse response, long elapsed) throws IOException {
        boolean printBody = Boolean.TRUE.equals(WindTracer.TRACER.getContextVariable(ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME));
        String requestBody = printBody ? readBody(request.getHeaders().getContentType(), body, body.length) : "-";
        String responseBody = printBody ? readResponseBody(response) : "-";
        String logFormat = """
                URI: {}
                Method: {}
                RequestHeaders: {}
                RequestBody: {}
                ResponseStatus: {}
                ResponseBody: {}
                ResponseHeaders: {}
                Elapsed: {}ms
                """;
        Object[] arguments = {
                request.getURI().getRawPath(), request.getMethod(), redactHeaders(request.getHeaders()), requestBody,
                response.getStatusCode(), responseBody, redactHeaders(response.getHeaders()), elapsed
        };
        if (printBody) {
            // 强制打印
            log.info(logFormat, arguments);
        } else {
            if (!Objects.equals(env, WindConstants.PROD)) {
                log.info(logFormat, arguments);
            } else if (response.getStatusCode().is2xxSuccessful()) {
                log.debug(logFormat, arguments);
            } else {
                log.error(logFormat, arguments);
            }
        }
    }

    private static String readResponseBody(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        if (shouldSkipBodyLog(contentType)) {
            return WindConstants.DASHED;
        }
        return readBody(contentType, response.getBody().readNBytes(MAX_BODY_LENGTH + 1), response.getHeaders().getContentLength());
    }

    private static String readBody(MediaType contentType, byte[] body, long contentLength) {
        if (shouldSkipBodyLog(contentType)) {
            return WindConstants.DASHED;
        }
        int printLength = Math.min(body.length, MAX_BODY_LENGTH);
        String content = new String(body, 0, printLength, StandardCharsets.UTF_8);
        return body.length <= MAX_BODY_LENGTH ? content : content + "\n...[truncated, body size = " + contentLength + ", print size = " + printLength + "]";
    }

    private static boolean shouldSkipBodyLog(MediaType contentType) {
        return contentType == null || LOGGABLE_BODY_MEDIA_TYPES.stream().noneMatch(mediaType -> mediaType.isCompatibleWith(contentType));
    }

    private static HttpHeaders redactHeaders(HttpHeaders headers) {
        HttpHeaders result = new HttpHeaders();
        headers.forEach((name, values) -> result.put(name, isSensitiveHeader(name) ? List.of(MASK_TEXT) : List.copyOf(values)));
        return result;
    }

    private static boolean isSensitiveHeader(String headerName) {
        String name = headerName.toLowerCase(Locale.ROOT);
        return SENSITIVE_HEADER_PARTS.stream().anyMatch(name::contains);
    }
}
