package com.wind.client.rest;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 统一打印请求日志，注意由于此拦截器读取了 ResponseBody 在设置 {@link org.springframework.http.client.ClientHttpRequestFactory} 时需要用
 * {@link org.springframework.http.client.BufferingClientHttpRequestFactory} 包装一下，保证响应体可以多次读取
 *
 * @author wuxp
 * @date 2026-01-22 14:15
 */
@Slf4j
public class ClientHttpRequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 手动开启请求日志（每请求上下文变量）
     */
    public static final String ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME = "api.request.log.print.enable";

    /**
     * 最大读取响应体字节数，防止大响应耗尽内存
     */
    private static final int MAX_BODY_LENGTH = 1024 * 2;

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
        StopWatch watch = new StopWatch();
        watch.start();
        ClientHttpResponse response = execution.execute(request, body);
        watch.stop();
        log(request, body, response, watch.getTotalTimeMillis());
        return response;
    }

    private void log(HttpRequest request, byte[] body, ClientHttpResponse response, long elapsed) throws IOException {
        String requestBody = new String(body, StandardCharsets.UTF_8);
        String responseBody = readResponseBody(response);
        String logFormat = """
                URI: {}
                Method: {}
                RequestHeaders: {}
                RequestBody: {}
                ResponseStatus: {}
                ResponseBody:{}
                ResponseHeaders: {}
                Elapsed: {}ms
                """;
        if (Boolean.TRUE.equals(WindTracer.TRACER.getContextVariable(ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME))) {
            // 强制打印
            log.info(logFormat, request.getURI(), request.getMethod(), request.getHeaders(), requestBody, response.getStatusCode(), responseBody, response.getHeaders(), elapsed);
        } else {
            if (!Objects.equals(env, WindConstants.PROD)) {
                log.info(logFormat, request.getURI(), request.getMethod(), request.getHeaders(), requestBody, response.getStatusCode(), responseBody, response.getHeaders(),
                        elapsed);
            } else if (response.getStatusCode().is2xxSuccessful()) {
                log.debug(logFormat, request.getURI(), request.getMethod(), request.getHeaders(), requestBody, response.getStatusCode(), responseBody, response.getHeaders(),
                        elapsed);
            } else {
                log.error(logFormat, request.getURI(), request.getMethod(), request.getHeaders(), requestBody, response.getStatusCode(), responseBody, response.getHeaders(),
                        elapsed);
            }
        }
    }

    private String readResponseBody(ClientHttpResponse response) throws IOException {
        // 2xx && 生产环境，仅打印部分内容（减少内存的消耗）
        boolean limitPrint = response.getStatusCode().is2xxSuccessful() && Objects.equals(env, WindConstants.PROD);
        if (limitPrint) {
            InputStream body = response.getBody();
            int readSize = Math.min(body.available(), MAX_BODY_LENGTH);
            byte[] bytes = IOUtils.toByteArray(body, readSize);
            long contentLength = response.getHeaders().getContentLength();
            String content = new String(bytes, StandardCharsets.UTF_8);
            return readSize < MAX_BODY_LENGTH ? content : content + "\n...[truncated, response size = " + contentLength + ", print size = " + bytes.length + "]";
        }
        return IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
    }

}
