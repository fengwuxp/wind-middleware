package com.wind.trace;

import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.core.WritableContextVariables;
import com.wind.sequence.SequenceGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * wind trace 上下文
 *
 * @author wuxp
 * @date 2026-03-13 10:18
 * @docs <a href="https://opentelemetry.io/docs/concepts/signals/traces">Traces</a>
 * @docs <a href="https://opentelemetry.io/docs/reference/specification/overview">Specification Overview</a>
 * @docs <a href="https://opentelemetry.io/docs/specs/otel/trace/api">Tracing API</a>
 **/
public final class WindTraceContext implements ReadonlyContextVariables {

    private final String traceId;

    private final String spanId;

    private final String parentSpanId;

    private final WritableContextVariables contextVariables;

    /**
     * 构造函数
     *
     * @param traceId          traceId
     * @param spanId           spanId
     * @param parentSpanId     父 spanId
     * @param contextVariables 上下文变量
     */
    private WindTraceContext(@NonNull String traceId, @NonNull String spanId, @Nullable String parentSpanId, @Nullable Map<String, Object> contextVariables) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.contextVariables = new ScopedWritableView(contextVariables == null ? new HashMap<>() : new HashMap<>(contextVariables));
    }

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
        return new WindTraceContext(traceId, TRACE_GENERATOR.next(), null, contextVariables);
    }

    /**
     * 创建子 trace
     *
     * @param parent 父 trace
     * @return trace
     */
    public static WindTraceContext child(@NonNull WindTraceContext parent) {
        AssertUtils.notNull(parent, "argument parent must not null");
        Map<String, Object> parentVariables = parent.getContextVariables();
        return new WindTraceContext(parent.traceId, TRACE_GENERATOR.next(), parent.spanId, parentVariables);
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    /**
     * 创建子 span
     *
     * @param newSpanId spanId
     * @return trace
     */
    public WindTraceContext nextSpan(@NonNull String newSpanId) {
        return new WindTraceContext(traceId, newSpanId, this.spanId, getContextVariables());
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables.getContextVariables();
    }

    /**
     * 获取可写变量
     *
     * @return 可写变量
     */
    @NonNull
    WritableContextVariables writeView() {
        return contextVariables;
    }


    private record ScopedWritableView(Map<String, Object> variables) implements WritableContextVariables {

        private ScopedWritableView(Map<String, Object> variables) {
            this.variables = variables;
        }

        @Override
        public @NonNull WritableContextVariables putVariable(@NonNull String name, @Nullable Object val) {
            if (val == null) {
                variables.remove(name);
            } else {
                variables.put(name, val);
            }
            return this;
        }

        @Override
        public @NonNull WritableContextVariables removeVariable(@NonNull String name) {
            variables.remove(name);
            return this;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            // 返回一个快照，避免外部遍历时内部修改引发 ConcurrentModificationException
            return Map.copyOf(variables);
        }
    }
}
