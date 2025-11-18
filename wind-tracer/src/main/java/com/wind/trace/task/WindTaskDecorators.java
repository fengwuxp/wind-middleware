package com.wind.trace.task;

import lombok.AllArgsConstructor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 默认任务装饰器枚举
 *
 * @author wuxp
 * @date 2025-11-18 13:56
 **/
@AllArgsConstructor
public enum WindTaskDecorators implements TaskDecorator {

    /**
     * 上下文传播任务装饰器
     */
    CONTEXT_PROPAGATION(ContextPropagationTaskDecorator.of()),

    /**
     * 仅 trace 当前任务，不传播上下文
     */
    TRACE_ONLY(TraceTaskDecorator.of());

    private final TaskDecorator decorator;

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        return decorator.decorate(runnable);
    }
}
