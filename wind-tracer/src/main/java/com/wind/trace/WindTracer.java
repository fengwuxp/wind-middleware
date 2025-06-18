package com.wind.trace;

import com.wind.core.WritableContextVariables;
import com.wind.trace.thread.WindThreadTracer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.Collections;
import java.util.Map;

/**
 * 请求 or 线程 tracer
 *
 * @author wuxp
 * @date 2023-12-29 10:13
 **/
public interface WindTracer extends WritableContextVariables {

    /**
     * 默认的 tracer
     */
    WindTracer TRACER = new WindThreadTracer();

    /**
     * 如果上下文中不存在 traceId 则生成
     *
     * @see #trace(String)
     */
    void trace();

    /**
     * 通过传入的 traceId 设置到上下文中，如果 traceId 为空，则生成新的 traceId
     *
     * @param traceId traceId 如果为空则生成新的 traceId
     */
    default void trace(@Null String traceId) {
        trace(traceId, Collections.emptyMap());
    }

    /**
     * 通过传入的 traceId、 contextVariables 设置到上下文中，如果 traceId 为空，则生成新的 traceId
     *
     * @param traceId          traceId 如果为空则生成新的 traceId
     * @param contextVariables trace 上下文变量
     */
    void trace(@Null String traceId, @NotNull Map<String, Object> contextVariables);

    /**
     * 获取线程上下文中的 traceId，若不存在则创建
     *
     * @return trace id
     */
    String getTraceId();

    /**
     * 清除 trace 上下文
     */
    void clear();
}


