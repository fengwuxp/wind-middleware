package com.wind.trace;

import com.wind.core.ReadonlyContextVariables;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 请求 or 线程 tracer
 *
 * @author wuxp
 * @date 2023-12-29 10:13
 **/
public interface WindTracer extends ScopeValueTracer, ReadonlyContextVariables {

    /**
     * 默认的 tracer
     */
    WindTracer TRACER = new WindThreadTracer();

    /**
     * 获取当前 Scope 中的 trace context
     *
     * @return trace context
     */
    Optional<WindTraceContext> currentContext();

    /**
     * 获取当前 Scope 中的 traceId
     *
     * @return traceId
     */
    default Optional<String> currentTraceId() {
        return currentContext().map(WindTraceContext::traceId);
    }

    /**
     * 获取当前 Scope 中的 spanId
     *
     * @return spanId
     */
    default Optional<String> currentSpanId() {
        return currentContext().map(WindTraceContext::spanId);
    }

    /**
     * 获取当前 Scope 中的 parentSpanId
     *
     * @return parentSpanId
     */
    default Optional<String> currentParentSpanId() {
        return currentContext().map(WindTraceContext::parentSpanId);
    }

    /**
     * 获取当前 Scope 中的 trace context
     *
     * @return trace context
     */
    @NonNull
    default WindTraceContext requireContext() {
        return currentContext().orElseThrow(() -> new IllegalStateException("No trace context bound to current scope"));
    }

    /**
     * 获取当前 Scope 中的 traceId
     *
     * @return traceId
     */
    @NonNull
    default String requireTraceId() {
        return requireContext().traceId();
    }

    /**
     * 获取当前 Scope 中的 spanId
     *
     * @return spanId
     */
    @NonNull
    default String requreSpanId() {
        return requireContext().spanId();
    }

    /**
     * 创建一个 {@link Runnable}，该 Runnable 在当前 Scope 中执行
     *
     * @param runnable 函数
     * @return 包装后的函数
     * @see #run(Runnable)
     */
    static Runnable wrap(Runnable runnable) {
        return () -> WindTracer.TRACER.run(runnable);
    }

    /**
     * 创建一个 {@link Callable}，该 Callable 在当前 Scope 中执行
     *
     * @param callable 函数
     * @return 包装后的函数
     * @see #call(Callable)
     */
    static <T> Callable<T> wrap(Callable<T> callable) {
        return () -> WindTracer.TRACER.call(callable);
    }

    /**
     * 如果上下文中不存在 traceId 则生成
     *
     * @see #trace(String)
     */
    @Deprecated(forRemoval = true, since = "java 25")
    void trace();

    /**
     * 通过传入的 traceId 设置到上下文中，如果 traceId 为空，则生成新的 traceId
     *
     * @param traceId traceId 如果为空则生成新的 traceId
     */
    @Deprecated(forRemoval = true, since = "java 25")
    default void trace(@Null String traceId) {
        trace(traceId, Collections.emptyMap());
    }

    /**
     * 通过传入的 traceId、 contextVariables 设置到上下文中，如果 traceId 为空，则生成新的 traceId
     *
     * @param traceId          traceId 如果为空则生成新的 traceId
     * @param contextVariables trace 上下文变量
     */
    @Deprecated(forRemoval = true, since = "java 25")
    void trace(@Null String traceId, @NotNull Map<String, Object> contextVariables);

    /**
     * 获取线程上下文中的 traceId，若不存在则创建
     *
     * @return trace id
     */
    @Deprecated(forRemoval = true, since = "java 25")
    @NotBlank
    String getTraceId();

    /**
     * 清除 trace 上下文
     */
    @Deprecated(forRemoval = true, since = "java 25")
    void clear();


}


