package com.wind.trace;

import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.sequence.SequenceGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * wind trace 上下文
 *
 * @param traceId          traceId
 * @param spanId           spanId
 * @param parentSpanId     parentSpanId
 * @param contextVariables 上下文变量
 * @author wuxp
 * @date 2026-03-13 10:18
 * @docs <a href="https://opentelemetry.io/docs/concepts/signals/traces">Traces</a>
 * @docs <a href="https://opentelemetry.io/docs/reference/specification/overview">Specification Overview</a>
 * @docs <a href="https://opentelemetry.io/docs/specs/otel/trace/api">Tracing API</a>
 **/
public record WindTraceContext(@NonNull String traceId,
                               @NonNull String spanId,
                               @Nullable String parentSpanId,
                               @NonNull Map<String, Object> contextVariables) implements ReadonlyContextVariables {
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
        return new WindTraceContext(traceId, TRACE_GENERATOR.next(), null, Map.of());
    }

    /**
     * 创建子 trace
     *
     * @param traceId traceId
     * @return trace
     */
    public static WindTraceContext withTrace(@Nullable String traceId) {
        return withTrace(traceId, Map.of());
    }

    /**
     * 创建子 trace
     *
     * @param traceId          traceId
     * @param contextVariables 上下文变量
     * @return trace
     */
    public static WindTraceContext withTrace(@Nullable String traceId, @Nullable Map<String, Object> contextVariables) {
        traceId = (traceId == null || traceId.isBlank()) ? TRACE_GENERATOR.next() : traceId;
        Map<String, Object> variables = contextVariables == null ? Map.of() : Map.copyOf(contextVariables);
        return new WindTraceContext(traceId, TRACE_GENERATOR.next(), null, variables);
    }

    /**
     * 创建子 trace
     *
     * @param parent 父 trace
     * @return trace
     */
    public static WindTraceContext child(@NonNull WindTraceContext parent) {
        AssertUtils.notNull(parent, "argument parent must not null");
        Map<String, Object> parentVariables = parent.contextVariables();
        return new WindTraceContext(parent.traceId, TRACE_GENERATOR.next(), parent.spanId, Map.copyOf(parentVariables));
    }

    /**
     * 创建子 span
     *
     * @param newSpanId spanId
     * @return trace
     */
    public WindTraceContext nextSpan(String newSpanId) {
        return new WindTraceContext(traceId, newSpanId, this.spanId, contextVariables);
    }

    /**
     * 添加 contextVariables（返回新对象）
     */
    public WindTraceContext withVariable(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newVariables = new HashMap<>(this.contextVariables);
        if (value == null) {
            newVariables.remove(key);
        } else {
            newVariables.put(key, value);
        }
        return new WindTraceContext(traceId, spanId, parentSpanId, Map.copyOf(newVariables));
    }

    /**
     * 批量添加 contextVariables
     *
     * @param variables contextVariables
     */
    @NonNull
    public WindTraceContext withVariables(@NonNull Map<String, ?> variables) {
        Map<String, Object> newVariables = new HashMap<>(this.contextVariables);
        newVariables.putAll(variables);
        return new WindTraceContext(traceId, spanId, parentSpanId, newVariables);
    }


    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

}
