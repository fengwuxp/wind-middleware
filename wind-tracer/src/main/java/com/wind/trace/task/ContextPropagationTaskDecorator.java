package com.wind.trace.task;

import com.wind.common.exception.AssertUtils;
import com.wind.trace.WindTraceContext;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;

import java.util.Collections;
import java.util.Map;

/**
 * 用于在线程切换时，将 trace 上下文信息复制到新的线程中
 *
 * @author wuxp
 * @date 2023-12-29 09:55
 **/
@Slf4j
public abstract class ContextPropagationTaskDecorator implements TaskDecorator {

    private final boolean printExceptionLog;

    protected ContextPropagationTaskDecorator(boolean printExceptionLog) {
        this.printExceptionLog = printExceptionLog;
    }

    protected ContextPropagationTaskDecorator() {
        // 默认不输出异常日志，由任务处理者输出
        this(false);
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable task) {
        AssertUtils.notNull(task, "argument task must not null");
        // 捕获当前 trace context
        WindTraceContext context = WindTracer.TRACER.currentContext().orElse(null);
        // 捕获业务上下文
        Map<String, Object> businessContext = snapshotContextVariables();
        Runnable execution = () -> {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, parent context = {}", context);
                }
                restoreContextVariables(businessContext);
                task.run();
            } catch (Throwable throwable) {
                if (printExceptionLog) {
                    log.error("execute task exception, message = {}", throwable.getMessage(), throwable);
                }
                throw throwable;
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, clear context contextVariables");
                }
                clearContextVariables();
            }
        };
        return WindTracer.wrap(execution);
    }

    /**
     * 快照当前线程的上下文
     *
     * @return 上下文变量
     */
    protected Map<String, Object> snapshotContextVariables() {
        return Collections.emptyMap();
    }

    /**
     * 线程切换，恢复上下文 ，traceId 也在 contextVariables 中
     *
     * @param contextVariables copy 的上下文
     */
    protected void restoreContextVariables(Map<String, Object> contextVariables) {
        if (log.isDebugEnabled()) {
            log.debug("task decorate, restore contextVariables = {}", contextVariables);
        }
    }

    /**
     * 清除线程上下文
     */
    protected void clearContextVariables() {

    }

    public static TaskDecorator of() {
        return new ContextPropagationTaskDecorator() {
        };
    }
}
