package com.wind.trace;


import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.util.IpAddressUtils;
import com.wind.sequence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

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
    private static final ThreadLocal<WindTraceContext> TRACE_CONTEXT = ThreadLocal.withInitial(() -> null);

    @Override
    public void run(@NonNull Runnable runnable) {
        try {
            Optional<WindTraceContext> context = currentContext();
            if (context.isPresent()) {
                // 创建子 trace
                runWithContext(context.get(), runnable);
            } else {
                runWithNewContext(runnable);
            }
        } catch (Exception e) {
            throw buildThrowsException(e);
        }
    }

    @Override
    public void runWithContext(@NonNull WindTraceContext context, @NonNull Runnable runnable) {
        WindTraceContext previous = TRACE_CONTEXT.get();
        try {
            bindContext(WindTraceContext.child(context));
            runnable.run();
        } finally {
            restore(previous);
        }
    }

    @Override
    public void runWithNewContext(@NonNull Runnable runnable) {
        WindTraceContext previous = TRACE_CONTEXT.get();
        try {
            bindContext(WindTraceContext.root());
            runnable.run();
        } finally {
            restore(previous);
        }
    }

    @Override
    public <T> T call(@NonNull Callable<T> callable) {
        try {
            Optional<WindTraceContext> context = currentContext();
            if (context.isPresent()) {
                // 创建子 trace
                return callWithContext(context.get(), callable);
            } else {
                return callWithNewContext(callable);
            }
        } catch (Exception e) {
            throw buildThrowsException(e);
        }
    }

    @Override
    public <T> T callWithContext(@NonNull WindTraceContext context, @NonNull Callable<T> callable) {
        WindTraceContext previous = TRACE_CONTEXT.get();
        try {
            bindContext(WindTraceContext.child(context));
            return callable.call();
        } catch (Exception e) {
            throw buildThrowsException(e);
        } finally {
            restore(previous);
        }
    }

    @Override
    public <T> T callWithNewContext(@NonNull Callable<T> callable) {
        WindTraceContext previous = TRACE_CONTEXT.get();
        try {
            bindContext(WindTraceContext.root());
            return callable.call();
        } catch (Exception e) {
            throw buildThrowsException(e);
        } finally {
            restore(previous);
        }
    }

    @Override
    public Optional<WindTraceContext> currentContext() {
        return Optional.ofNullable(TRACE_CONTEXT.get());
    }

    private static void bindContext(WindTraceContext context) {
        TRACE_CONTEXT.set(context);
        TraceMdcBridge.rebind(context);
    }

    private void restore(WindTraceContext previous) {
        if (previous == null) {
            clear();
            return;
        }
        bindContext(previous);
    }

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

    private void putVariable(@NonNull String name, Object val) {
        AssertUtils.hasText(name, "argument name must not empty");
        if (val != null) {
            TRACE_CONTEXT.set(getContext().withVariable(name, val));
        }
        if (val instanceof String str) {
            // 字符传类型变量同步到 MDC 中
            MDC.put(name, str);
        }

    }

    @Override
    public void clear() {
        TraceMdcBridge.clear();
        TRACE_CONTEXT.remove();
    }

    private Map<String, Object> requireVariables() {
        WindTraceContext context = getContext();
        return context.getContextVariables();
    }

    @Deprecated
    private static @NonNull WindTraceContext getContext() {
        WindTraceContext context = TRACE_CONTEXT.get();
        if (context == null) {
            context = WindTraceContext.root();
            bindContext(context);
        }
        return context;
    }

    private static @NonNull BaseException buildThrowsException(Exception e) {
        if (e instanceof BaseException exception) {
            return exception;
        }
        return new BaseException(DefaultExceptionCode.COMMON_ERROR, "trace call func exception", e);
    }
}
