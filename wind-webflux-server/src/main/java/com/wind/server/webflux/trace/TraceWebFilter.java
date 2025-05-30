package com.wind.server.webflux.trace;


import com.wind.common.WindConstants;
import com.wind.common.util.IpAddressUtils;
import com.wind.common.util.ServiceInfoUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.server.webflux.util.ServerWebExchangeResponseUtils;
import com.wind.trace.WindTracer;
import com.wind.trace.reactor.WindTracerContextHooks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.wind.common.WindConstants.HTTP_REQUEST_URL_TRACE_NAME;
import static com.wind.common.WindConstants.LOCAL_HOST_IP_V4;
import static com.wind.common.WindConstants.WIND_TRANCE_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.CLIENT_IP_HEAD_NAMES;
import static com.wind.common.WindHttpConstants.HTTP_HOST_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_CLIENT_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_HOST_ATTRIBUTE_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;
import static com.wind.common.WindHttpConstants.HTTP_USER_AGENT_HEADER_NAME;
import static com.wind.common.WindHttpConstants.REAL_SERVER_IP;

/**
 * trace filter
 *
 * @author wuxp
 * @date 2025-05-29 12:40
 **/
@Slf4j
public class TraceWebFilter implements WebFilter {

    static {
        WindTracerContextHooks.registerHook();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        if (!ServiceInfoUtils.isOnline()) {
            response.getHeaders().set(REAL_SERVER_IP, IpAddressUtils.getLocalIpv4WithCache());
        }

        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(WIND_TRANCE_ID_HEADER_NAME);
        Map<String, Object> contextVariables = new HashMap<>();
        // 将用户请求来源 ip 并设置到请求上下文中
        String requestSourceIp = getRequestSourceIp(request);
        exchange.getAttributes().put(HTTP_REQUEST_IP_ATTRIBUTE_NAME, requestSourceIp);
        contextVariables.put(HTTP_REQUEST_IP_ATTRIBUTE_NAME, requestSourceIp);
        contextVariables.put(HTTP_REQUEST_HOST_ATTRIBUTE_NAME, getRequestSourceHost(request));
        contextVariables.put(HTTP_REQUEST_URL_TRACE_NAME, request.getURI().getPath());
        contextVariables.put(HTTP_USER_AGENT_HEADER_NAME, request.getHeaders().getFirst(HTTP_USER_AGENT_HEADER_NAME));
        contextVariables.put(HTTP_REQUEST_CLIENT_ID_HEADER_NAME, request.getHeaders().getFirst(HTTP_REQUEST_CLIENT_ID_HEADER_NAME));
        contextVariables.put(LOCAL_HOST_IP_V4, IpAddressUtils.getLocalIpv4WithCache());
        WindTracer.TRACER.trace(traceId, contextVariables);
        // 提前写入 traceId 到响应头，避免 response committed 后无法写回
        response.getHeaders().add(WIND_TRANCE_ID_HEADER_NAME, WindTracer.TRACER.getTraceId());

        return chain.filter(exchange)
                .contextWrite(context -> {
                    context.putAllMap(contextVariables);
                    return context;
                })
                .doOnError(throwable -> {
                    log.error("request error, cause by = {}", throwable.getMessage(), throwable);
                    // 写回错误响应 JSON
                    ServerWebExchangeResponseUtils.writeApiResp(exchange, RestfulApiRespFactory.withThrowable(throwable));
                })
                .doFinally(signal -> WindTracer.TRACER.clear());
    }

    private String getRequestSourceIp(ServerHttpRequest request) {
        for (String headerName : CLIENT_IP_HEAD_NAMES) {
            List<String> ips = request.getHeaders().get(headerName);
            if (ips != null && !ips.isEmpty()) {
                String ip = ips.get(0).trim();
                if (!ip.isEmpty()) {
                    return ip.split(",", 2)[0];
                }
            }
        }
        return Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
    }

    private String getRequestSourceHost(ServerHttpRequest request) {
        try {
            String host = request.getHeaders().getFirst(HTTP_HOST_HEADER_NAME);
            return host != null ? host : URI.create(request.getURI().toString()).getHost();
        } catch (Exception exception) {
            log.error("get host error: {}", request.getURI(), exception);
            return WindConstants.UNKNOWN;
        }
    }


//    /**
//     * Creates scope passing span operator which applies only to not
//     * {@code Scannable.Attr.RunStyle.SYNC} {@code Publisher}s. Used by
//     * {@code InstrumentationType#DECORATE_ON_EACH}
//     *
//     * @param springContext the Spring context.
//     * @param <T>           an arbitrary type that is left unchanged by the span operator.
//     * @return operator to apply to {@link Hooks#onEachOperator(Function)}.
//     */
//    public static <T> Function<? super Publisher<T>, ? extends Publisher<T>> onEachOperatorForOnEachInstrumentation(
//            ConfigurableApplicationContext springContext) {
//        if (log.isTraceEnabled()) {
//            log.trace("Scope passing operator [" + springContext + "]");
//        }
//
//        // keep a reference outside the lambda so that any caching will be visible to
//        // all publishers
//        LazyBean<CurrentTraceContext> lazyCurrentTraceContext = LazyBean.create(springContext,
//                CurrentTraceContext.class);
//
//        LazyBean<Tracer> lazyTracer = LazyBean.create(springContext, Tracer.class);
//
//        @SuppressWarnings("rawtypes")
//        Predicate<Publisher> shouldDecorate = ReactorHooksHelper::shouldDecorate;
//        @SuppressWarnings("rawtypes")
//        BiFunction<Publisher, ? super CoreSubscriber<? super T>, ? extends CoreSubscriber<? super T>> lifter = new ScopeTraceContextSubscriber();
//        return Operators.liftPublisher(shouldDecorate, named(ReactorHooksHelper.LIFTER_NAME, lifter));
//    }
//
//    private static <T> Context context(CoreSubscriber<? super T> sub) {
//        try {
//            return sub.currentContext();
//        }
//        catch (Exception ex) {
//            if (log.isDebugEnabled()) {
//                log.debug("Exception occurred while trying to retrieve the context", ex);
//            }
//        }
//        return Context.empty();
//    }
}
