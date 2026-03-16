//package com.wind.trace;
//
//import com.wind.common.exception.BaseException;
//import com.wind.common.exception.DefaultExceptionCode;
//import com.wind.core.WritableContextVariables;
//import org.jspecify.annotations.NonNull;
//import org.jspecify.annotations.Nullable;
//
//import java.lang.ScopedValue;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.Callable;
//
///**
// * 基于 {@link ScopedValue} 的 Tracer
// *
// * @author wuxp
// * @date 2026-03-13 10:04
// **/
//final class DefaultScopeValueTracer implements WindTracer {
//
//    /**
//     * 线程 trace context
//     *
//     * @since 25
//     */
//    private static final ScopedValue<WindTraceContext> TRACE_CONTEXT = ScopedValue.newInstance();
//
//    @Override
//    public void trace() {
//        throw new UnsupportedOperationException("not support");
//    }
//
//    @Override
//    public void trace(String traceId, Map<String, Object> contextVariables) {
//        throw new UnsupportedOperationException("not support");
//    }
//
//    @Override
//    public String getTraceId() {
//        throw new UnsupportedOperationException("not support");
//    }
//
//    @Override
//    public void clear() {
//        throw new UnsupportedOperationException("not support");
//    }
//
//    @Override
//    public void run(@NonNull Runnable runnable) {
//        Optional<WindTraceContext> context = currentContext();
//        try {
//            if (context.isPresent()) {
//                // 创建子 trace
//                ScopedValue.where(TRACE_CONTEXT, WindTraceContext.child(context.get())).run(runnable);
//            } else {
//                ScopedValue.where(TRACE_CONTEXT, WindTraceContext.root()).run(runnable);
//            }
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public void runWithTraceId(@NonNull String traceId, @NonNull Runnable runnable) {
//        try {
//            ScopedValue.where(TRACE_CONTEXT, WindTraceContext.trace(traceId)).run(runnable);
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public void runWithTraceContext(@NonNull WindTraceContext parent, @NonNull Runnable runnable) {
//        try {
//            ScopedValue.where(TRACE_CONTEXT, WindTraceContext.child(parent)).run(runnable);
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public <T> T call(@NonNull Callable<T> callable) {
//        try {
//            Optional<WindTraceContext> context = currentContext();
//            if (context.isPresent()) {
//                // 创建子 trace
//                return ScopedValue.where(TRACE_CONTEXT, WindTraceContext.child(context.get())).call(callable::call);
//            } else {
//                return ScopedValue.where(TRACE_CONTEXT, WindTraceContext.root()).call(callable::call);
//            }
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public @Nullable <T> T callWithTraceId(@NonNull String traceId, @NonNull Callable<T> callable) {
//        try {
//            return ScopedValue.where(TRACE_CONTEXT, WindTraceContext.trace(traceId)).call(callable::call);
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public @Nullable <T> T callWithTraceContext(@NonNull WindTraceContext parent, @NonNull Callable<T> callable) {
//        try {
//            return ScopedValue.where(TRACE_CONTEXT, WindTraceContext.child(parent)).call(callable::call);
//        } catch (Exception e) {
//            throw buildThrowsException(e);
//        }
//    }
//
//    @Override
//    public Optional<String> currentTraceId() {
//        return currentContext().map(WindTraceContext::traceId);
//    }
//
//    @Override
//    public Optional<WindTraceContext> currentContext() {
//        if (TRACE_CONTEXT.isBound()) {
//            return Optional.of(TRACE_CONTEXT.get());
//        }
//        return Optional.empty();
//    }
//
//    private static @NonNull BaseException buildThrowsException(Exception e) {
//        if (e instanceof BaseException exception) {
//            return exception;
//        }
//        return new BaseException(DefaultExceptionCode.COMMON_ERROR, "trace call func exception", e);
//    }
//
//    @Override
//    public @NonNull WritableContextVariables putVariable(@NonNull String name, @Nullable Object val) {
//        return requireContext().putVariable(name, val);
//    }
//
//    @Override
//    public @NonNull WritableContextVariables removeVariable(@NonNull String name) {
//        requireContext().removeVariable(name);
//        return this;
//    }
//
//    @Override
//    public @NonNull Map<String, Object> getContextVariables() {
//        return requireContext().getContextVariables();
//    }
//}
