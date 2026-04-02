package com.wind.trace;

import com.wind.core.WritableContextVariables;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 上下文 tracer
 *
 * @author wuxp
 * @date 2023-12-29 10:13
 **/
public interface WindTracer extends ScopeValueTracer, WritableContextVariables {

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
    default String requireSpanId() {
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

}


