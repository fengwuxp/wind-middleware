package com.wind.common.exception;

import jakarta.validation.constraints.NotNull;

import java.io.Serial;

/**
 * 执行任务包装异常，{@link com.wind.common.function.WindFunctions}
 *
 * @author wuxp
 * @date 2025-10-13 15:13
 **/
public class ExecutionWrapperException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -69962151270633546L;

    public ExecutionWrapperException(String message) {
        super(message);
    }

    public ExecutionWrapperException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 包装原始异，对于某些场景关注原始异常的，可以识别该异常通过 {@link #getCause()} 获取原始异常
     *
     * @param cause 原始异常
     * @return WindIdempotentException
     */
    public static ExecutionWrapperException withThrows(@NotNull Throwable cause) {
        throw new ExecutionWrapperException("wrapper execute cause throwable", cause);
    }
}
