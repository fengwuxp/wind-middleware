package com.wind.trace.task;

import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.jspecify.annotations.NonNull;

/**
 * 仅 trace 任务，不做线程之间的上下文传递
 *
 * @author wuxp
 * @date 2025-11-18 13:48
 **/
@Slf4j
public record TraceTaskDecorator(boolean printExceptionLog) implements TaskDecorator {

    public static TaskDecorator of() {
        // 默认不输出异常日志，由任务处理者输出
        return new TraceTaskDecorator(false);
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        return () -> {
            try {
                WindTracer.TRACER.trace();
                runnable.run();
            } catch (Throwable throwable) {
                if (printExceptionLog) {
                    log.error("execute task exception, message = {}", throwable.getMessage(), throwable);
                }
                throw throwable;
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, clear trace context");
                }
                // 清除线程上下文
                WindTracer.TRACER.clear();
            }
        };
    }
}
