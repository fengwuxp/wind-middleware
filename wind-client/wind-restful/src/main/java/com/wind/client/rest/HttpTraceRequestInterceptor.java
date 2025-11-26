package com.wind.client.rest;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 从上下文中设置 traceId 到 http 请求头中
 *
 * @author wuxp
 * @date 2024-01-30 15:37
 **/
public record HttpTraceRequestInterceptor(String traceHeaderName) implements ClientHttpRequestInterceptor {

    public HttpTraceRequestInterceptor() {
        this(WindConstants.WIND_TRANCE_ID_HEADER_NAME);
    }

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        String traceId = WindTracer.TRACER.getTraceId();
        if (StringUtils.hasText(traceId) && !request.getHeaders().containsHeader(traceHeaderName)) {
            request.getHeaders().add(traceHeaderName, traceId);
        }
        return execution.execute(request, body);
    }
}
