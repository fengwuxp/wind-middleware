package com.wind.trace.thread;


import com.wind.common.util.IpAddressUtils;
import com.wind.core.WritableContextVariables;
import com.wind.sequence.SequenceGenerator;
import com.wind.trace.WindTracer;
import org.slf4j.MDC;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.wind.common.WindConstants.LOCAL_HOST_IP_V4;
import static com.wind.common.WindConstants.TRACE_ID_NAME;

/**
 * 线程上下文 trace工具类
 *
 * @author wuxp
 * @date 2023-12-29 09:57
 **/
public final class WindThreadTracer implements WindTracer {

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
        trace(getTraceId());
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
        putVariable(LOCAL_HOST_IP_V4, IpAddressUtils.getLocalIpv4WithCache());
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
        return Collections.unmodifiableMap(new HashMap<>(nullSecurityGetVariables()));
    }

    @Override
    public WritableContextVariables putVariable(String name, Object val) {
        if (name != null && val != null) {
            // TODO 待优化
            nullSecurityGetVariables().put(name, val);
            if (val instanceof String) {
                // 字符传类型变量同步到 MDC 中
                MDC.put(name, (String) val);
            }
        }
        return this;
    }

    @Override
    public WritableContextVariables removeVariable(String name) {
        nullSecurityGetVariables().remove(name);
        return this;
    }

    @Override
    public void clear() {
        MDC.clear();
        nullSecurityGetVariables().clear();
        TRACE_CONTEXT.remove();
    }

    private Map<String, Object> nullSecurityGetVariables() {
        Map<String, Object> variables = TRACE_CONTEXT.get();
        if (variables == null) {
            variables = new ConcurrentHashMap<>();
            TRACE_CONTEXT.set(variables);
        }
        return variables;
    }
}
