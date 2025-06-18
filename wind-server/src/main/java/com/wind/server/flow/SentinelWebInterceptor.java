package com.wind.server.flow;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.metric.extension.MetricExtensionProvider;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.sentinel.SentinelResource;
import com.wind.sentinel.SentinelResourcesType;
import com.wind.sentinel.metrics.SentinelMetricsCollector;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import com.wind.web.util.HttpServletRequestUtils;
import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * sentinel web interceptor
 *
 * @author wuxp
 * @date 2024-03-07 17:28
 **/
@Slf4j
@AllArgsConstructor
public class SentinelWebInterceptor implements HandlerInterceptor {

    @VisibleForTesting
    static final String DEFAULT_SENTINEL_ENTRY_ATTRIBUTE_NAME = SentinelWebInterceptor.class.getName() + ".entry";

    static {
        // 增加自定义的指标收集器
        MetricExtensionProvider.addMetricExtension(new SentinelMetricsCollector(SentinelResourcesType.HTTP_API.getTypeName()));
    }

    private final Function<HttpServletRequest, SentinelResource> resourceProvider;

    private final SentinelBlockExceptionHandler blockExceptionHandler;

    private final String entryAttributeName;

    public SentinelWebInterceptor(Function<HttpServletRequest, SentinelResource> resourceProvider, String entryAttributeName) {
        this(resourceProvider, SentinelWebInterceptor::defaultHandleBlockException, entryAttributeName);
    }

    public SentinelWebInterceptor(Function<HttpServletRequest, SentinelResource> resourceProvider) {
        this(resourceProvider, DEFAULT_SENTINEL_ENTRY_ATTRIBUTE_NAME);
    }

    public static SentinelWebInterceptor defaults(Function<HttpServletRequest, SentinelResource> resourceProvider) {
        return new SentinelWebInterceptor(resourceProvider);
    }

    /**
     * 拦截所有资源请求
     */
    public static SentinelWebInterceptor all(Function<HttpServletRequest, SentinelResource> resourceProvider) {
        return new SentinelWebInterceptor(resourceProvider, DEFAULT_SENTINEL_ENTRY_ATTRIBUTE_NAME + ".all");
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        SentinelResource resource = resourceProvider.apply(request);
        if (resource == null || ObjectUtils.isEmpty(resource.getName())) {
            log.debug("no resource found in request, request uri = {}", request.getRequestURI());
            return true;
        }
        ContextUtil.enter(resource.getContextName(), resource.getOrigin());
        try {
            List<Object> args = new ArrayList<>(resource.getArgs());
            args.add(Tags.of(resource.getMetricsTags()));
            Entry entry = SphU.entry(resource.getName(), resource.getResourceType(), resource.getEntryType(), args.toArray(new Object[0]));
            request.setAttribute(entryAttributeName, entry);
        } catch (BlockException exception) {
            try {
                blockExceptionHandler.handle(request, response, exception);
            } finally {
                ContextUtil.exit();
            }
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler,
                                Exception exception) {
        Entry entry = HttpServletRequestUtils.getRequestAttribute(entryAttributeName, request);
        if (entry == null) {
            // should not happen
            log.warn("no entry found in request, key = {}", entryAttributeName);
            return;
        }
        entry.exit();
        request.removeAttribute(entryAttributeName);
        if (exception != null) {
            Tracer.traceEntry(exception, entry);
        }
        ContextUtil.exit();
    }

    private static void defaultHandleBlockException(HttpServletRequest request, HttpServletResponse response, BlockException exception) {
        String message = SpringI18nMessageUtils.getMessage(DefaultExceptionCode.TO_MANY_REQUESTS.getDesc());
        HttpResponseMessageUtils.writeApiResp(response, RestfulApiRespFactory.toManyRequests(message));
    }
}
