package com.wind.trace;


import com.wind.common.exception.AssertUtils;
import com.wind.common.util.IpAddressUtils;
import com.wind.core.WritableContextVariables;
import com.wind.sequence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.wind.common.WindConstants.LOCALHOST_IP_V4;
import static com.wind.common.WindConstants.TRACE_ID_NAME;

/**
 * 线程上下文 trace工具类
 *
 * @author wuxp
 * @date 2023-12-29 09:57
 **/
final class WindThreadTracer implements WindTracer {

    /**
     * traceId 生成器
     */
    private static final SequenceGenerator TRACE_GENERATOR = () -> SequenceGenerator.randomAlphanumeric(32);

    /**
     * 线程 trace context
     */
    private static final ThreadLocal<Map<String, Object>> TRACE_CONTEXT = ThreadLocal.withInitial(ConcurrentHashMap::new);

    @Override
    public void trace() {
        trace(null);
    }

    @Override
    public void trace(String traceId) {
        trace(traceId, Collections.emptyMap());
    }

    @Override
    public void trace(String traceId, @NotNull Map<String, Object> contextVariables) {
        if (traceId == null) {
            traceId = (String) contextVariables.get(TRACE_ID_NAME);
        }
        putVariable(TRACE_ID_NAME, traceId == null ? TRACE_GENERATOR.next() : traceId);
        putVariable(LOCALHOST_IP_V4, IpAddressUtils.getLocalIpv4WithCache());
        Objects.requireNonNull(contextVariables, "argument contextVariables must not null").forEach(this::putVariable);
    }

    @Override
    public String getTraceId() {
        String result = getContextVariable(TRACE_ID_NAME);
        if (result == null) {
            // 没有则生成
            result = TRACE_GENERATOR.next();
            putVariable(TRACE_ID_NAME, result);
        }
        return result;
    }

    @Override
    public Map<String, Object> getContextVariables() {
        // 返回一个快照，避免外部遍历时内部修改引发 ConcurrentModificationException
        return Map.copyOf(requireVariables());
    }

    @Override
    @NonNull
    public WritableContextVariables putVariable(@NonNull String name, Object val) {
        AssertUtils.hasText(name, "argument name must not empty");
        if (val != null) {
            requireVariables().put(name, val);
            if (val instanceof String str) {
                // 字符传类型变量同步到 MDC 中
                MDC.put(name, str);
            }
        }
        return this;
    }

    @Override
    @NonNull
    public WritableContextVariables removeVariable(@NonNull String name) {
        requireVariables().remove(name);
        return this;
    }

    @Override
    public void clear() {
        MDC.clear();
        requireVariables().clear();
        TRACE_CONTEXT.remove();
    }

    private Map<String, Object> requireVariables() {
        Map<String, Object> variables = TRACE_CONTEXT.get();
        if (variables == null) {
            variables = new ConcurrentHashMap<>();
            TRACE_CONTEXT.set(variables);
        }
        return variables;
    }
}
