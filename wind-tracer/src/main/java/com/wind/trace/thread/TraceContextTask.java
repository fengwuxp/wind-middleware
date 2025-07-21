package com.wind.trace.thread;

import com.wind.common.exception.AssertUtils;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Map;

/**
 * 用于在线程切换时，将 trace 上下文信息复制到新的线程中
 *
 * @author wuxp
 * @date 2023-12-29 09:55
 **/
@Slf4j
public abstract class TraceContextTask implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable task) {
        AssertUtils.notNull(task, "argument task must not null");
        // 获取当前线程的上下文
        Map<String, Object> middlewareContext = WindTracer.TRACER.getContextVariables();
        Map<String, Object> businessContextVariables = copyContextVariables();
        return () -> {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, trace context: {}", middlewareContext);
                }
                // 线程切换，复制上下文 ，traceId 也在 contextVariables 中
                WindTracer.TRACER.trace(null, middlewareContext);
                traceContextVariables(businessContextVariables);
                traceContext();
                task.run();
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, clear trace context");
                }
                // 清除线程上下文
                WindTracer.TRACER.clear();
                clearContextVariables();
                clearContext();
            }
        };
    }

    @Deprecated
    protected void traceContext() {
    }

    @Deprecated
    protected void clearContext() {
    }

    /**
     * 复制当前线程的上下文
     *
     * @return 上下文变量
     */
    protected Map<String, Object> copyContextVariables() {
        return Collections.emptyMap();
    }

    /**
     * 线程切换，复制上下文 ，traceId 也在 contextVariables 中
     *
     * @param contextVariables 上下文
     */
    protected void traceContextVariables(Map<String, Object> contextVariables) {

    }

    /**
     * 清除线程上下文
     */
    protected void clearContextVariables() {

    }

    public static TaskDecorator of() {
        return new TraceContextTask() {
        };
    }
}
