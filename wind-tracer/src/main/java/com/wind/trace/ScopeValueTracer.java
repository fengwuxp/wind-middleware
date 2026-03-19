package com.wind.trace;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Callable;

/**
 * Trace 上下文的 Scope 级管理器。
 *
 * <p>
 * 该接口用于在当前执行 Scope 中创建、传播和访问 {@link WindTraceContext}，
 * 为应用提供统一的 Trace 上下文生命周期管理能力。
 * </p>
 *
 * <p>
 * 主要职责：
 * <ul>
 *     <li>在执行函数时创建或继承 {@link WindTraceContext}</li>
 *     <li>建立父子 Trace 关系，实现 Trace 的层级传播</li>
 *     <li>在当前执行 Scope 内安全地绑定 TraceContext</li>
 *     <li>提供访问当前 TraceContext 与 TraceId 的能力</li>
 * </ul>
 * </p>
 *
 * <p>
 * 典型使用场景：
 * <ul>
 *     <li>请求链路追踪（TraceId / Span）</li>
 *     <li>日志 MDC 上下文传播</li>
 *     <li>异步任务 Trace 传递</li>
 * </ul>
 * </p>
 *
 * <p>
 * 实现通常基于：
 * <ul>
 *     <li>{@link ScopedValue}（推荐，JDK25+）</li>
 *     <li>{@link ThreadLocal}（兼容实现）</li>
 * </ul>
 * </p>
 *
 * @author wuxp
 * @since 2026-03-13
 */
public interface ScopeValueTracer {

    /**
     * 在当前 Scope 中，执行函数，
     * 1. 如果 {@link ScopedValue} 值存在则使用 Scope 中的 trace contex {@link WindTraceContext#child(WindTraceContext)}
     * 2. 如果 {@link ScopedValue} 值不存在则创建一个新的 trace contex {@link WindTraceContext#root()}
     *
     * @param runnable 执行的函数
     */
    void run(@NonNull Runnable runnable);

    /**
     * 在当前 Scope 中，执行函数
     *
     * @param context  当前 scope 使用的 trace context
     * @param runnable 执行的函数
     * @see WindTraceContext#child(WindTraceContext)
     */
    void runWithContext(@NonNull WindTraceContext context, @NonNull Runnable runnable);

    /**
     * 创建新的 Scope Context，并执行函数
     *
     * @param runnable 函数
     */
    void runWithNewContext(@NonNull Runnable runnable);

    /**
     * 在当前 Scope 中，执行函数
     *
     * @param callable 执行的函数
     * @param <T>      返回值类型
     * @return 返回值
     */
    <T> T call(@NonNull Callable<T> callable);

    /**
     * 在当前 Scope 中，执行函数
     *
     * @param context  当前 scope 使用的 trace context
     * @param callable 执行的函数
     * @see WindTraceContext#child(WindTraceContext)
     */
    <T> T callWithContext(@NonNull WindTraceContext context, @NonNull Callable<T> callable);

    /**
     * 创建新的 Scope Context，并执行函数
     *
     * @param callable 函数
     */
    <T> T callWithNewContext(@NonNull Callable<T> callable);

}
