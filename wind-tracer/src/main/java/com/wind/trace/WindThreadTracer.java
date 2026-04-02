package com.wind.trace;


import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.core.WritableContextVariables;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 线程上下文 trace工具类
 *
 * @author wuxp
 * @date 2023-12-29 09:57
 **/
final class WindThreadTracer implements WindTracer {

    /**
     * 线程 trace context
     */
    private static final ThreadLocal<WindTraceContext> TRACE_CONTEXT = ThreadLocal.withInitial(() -> null);

    @Override
    public void run(@NonNull Runnable runnable) {
        Optional<WindTraceContext> context = currentContext();
        if (context.isPresent()) {
            // 创建子 trace
            runWithContext(context.get(), runnable);
        } else {
            runWithNewContext(runnable);
        }
    }

    @Override
    public void runWithContext(@NonNull WindTraceContext context, @NonNull Runnable runnable) {
        WindTraceContext previous = TRACE_CONTEXT.get();
        try {
            bindContext(WindTraceContext.child(context));
            runnable.run();
        } catch (Exception e) {
            throw buildThrowsException(e);
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
        } catch (Exception e) {
            throw buildThrowsException(e);
        } finally {
            restore(previous);
        }
    }

    @Override
    public <T> T call(@NonNull Callable<T> callable) {
        Optional<WindTraceContext> context = currentContext();
        if (context.isPresent()) {
            // 创建子 trace
            return callWithContext(context.get(), callable);
        } else {
            return callWithNewContext(callable);
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
            // clear trace context
            TRACE_CONTEXT.remove();
            TraceMdcBridge.clear();
            return;
        }
        bindContext(previous);
    }

    @Override
    @NonNull
    public Map<String, Object> getContextVariables() {
        return writeView().getContextVariables();
    }

    @Override
    @NonNull
    public WritableContextVariables putVariable(@NonNull String name, Object val) {
        AssertUtils.hasText(name, "argument name must not empty");
        if (val != null) {
            writeView().putVariable(name, val);
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
        writeView().removeVariable(name);
        return this;
    }

    private WritableContextVariables writeView() {
        WindTraceContext context = TRACE_CONTEXT.get();
        if (context == null) {
            context = WindTraceContext.root();
            bindContext(context);
        }
        return context.writeView();
    }

    private static @NonNull BaseException buildThrowsException(Exception e) {
        if (e instanceof BaseException exception) {
            return exception;
        }
        return new BaseException(DefaultExceptionCode.COMMON_ERROR, "wrap trace run func exception", e);
    }
}
