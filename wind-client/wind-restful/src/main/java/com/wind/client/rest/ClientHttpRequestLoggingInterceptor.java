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
        String logText = String.format("""
                URI: %s
                Method: %s
                RequestHeaders: %s
                RequestBody: %s
                ResponseStatus: %s
                ResponseBody: %s
                Elapsed: %dms
                """, request.getURI(), request.getMethod(), request.getHeaders(), requestBody, response.getStatusCode(), responseBody, elapsed);
        if (Boolean.TRUE.equals(WindTracer.TRACER.getContextVariable(ENABLE_API_REQUEST_LOG_PRINT_VARIABLE_NAME))) {
            // 强制打印
            log.info(logText);
        } else {
            if (!Objects.equals(env, WindConstants.PROD)) {
                log.info(logText);
            } else if (response.getStatusCode().is2xxSuccessful()) {
                log.debug(logText);
            } else {
                log.error(logText);
            }
        }
    }

    private String readResponseBody(ClientHttpResponse response) throws IOException {
        // 2xx && 生产环境，仅打印部分内容（减少内存的消耗）
        boolean limitPrint = response.getStatusCode().is2xxSuccessful() && Objects.equals(env, WindConstants.PROD);
        if (limitPrint) {
            byte[] bytes = IOUtils.toByteArray(response.getBody(), MAX_BODY_LENGTH);
            long contentLength = response.getHeaders().getContentLength();
            return new String(bytes, StandardCharsets.UTF_8) + "\n...[truncated, response size = " + contentLength + ", print size = " + bytes.length + "]";
        }
        return IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
    }

}
