package com.wind.trace;

import com.wind.common.exception.AssertUtils;
import com.wind.core.WritableContextVariables;
import com.wind.sequence.SequenceGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * wind trace 上下文
 *
 * @param traceId      traceId
 * @param spanId       spanId
 * @param parentSpanId parentSpanId
 * @param variables    上下文变量
 * @author wuxp
 * @date 2026-03-13 10:18
 * @docs <a href="https://opentelemetry.io/docs/concepts/signals/traces">Traces</a>
 * @docs <a href="https://opentelemetry.io/docs/reference/specification/overview">Specification Overview</a>
 * @docs <a href="https://opentelemetry.io/docs/specs/otel/trace/api">Tracing API</a>
 **/
public record WindTraceContext(String traceId, String spanId, String parentSpanId, Map<String, Object> variables) implements WritableContextVariables {

    /**
     * traceId 生成器
     */
    private static final SequenceGenerator TRACE_GENERATOR = () -> SequenceGenerator.randomAlphanumeric(32);

    /**
     * 创建根 trace
     *
     * @return trace
     */
    public static WindTraceContext root() {
        String traceId = TRACE_GENERATOR.next();
        return new WindTraceContext(traceId, TRACE_GENERATOR.next(), null, new ConcurrentHashMap<>());
    }

    /**
     * 创建子 trace
     *
     * @param traceId traceId
     * @return trace
     */
    public static WindTraceContext trace(@NonNull String traceId) {
        AssertUtils.hasText(traceId, "argument traceId must not empty");
        return new WindTraceContext(traceId, TRACE_GENERATOR.next(), null, new ConcurrentHashMap<>());
    }

    /**
     * 创建子 trace
     *
     * @param parent 父 trace
     * @return trace
     */
    public static WindTraceContext child(@NonNull WindTraceContext parent) {
        AssertUtils.notNull(parent, "argument parent must not null");
        Map<String, Object> parentVariables = parent.variables();
        return new WindTraceContext(parent.traceId, TRACE_GENERATOR.next(), parent.spanId, new ConcurrentHashMap<>(parentVariables));
    }

    /**
     * 创建子 span
     *
     * @param newSpanId spanId
     * @return trace
     */
    public WindTraceContext nextSpan(String newSpanId) {
        return new WindTraceContext(traceId, newSpanId, this.parentSpanId, variables);
    }

    @Override
    public @NonNull WritableContextVariables putVariable(@NonNull String name, @Nullable Object val) {
        variables.put(name, val);
        if (val instanceof String str) {
            // 字符传类型变量同步到 MDC 中   TODO MDC Scope 兼容问题
            MDC.put(name, str);
        }
        return this;
    }

    @Override
    public @NonNull WritableContextVariables removeVariable(@NonNull String name) {
        variables.remove(name);
        MDC.remove(name);
        return this;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return variables;
    }
}
