package com.wind.middleware.idempotent;

import java.io.Serial;

/**
 * 幂等服务执行异常
 *
 * @author wuxp
 * @date 2025-10-13 15:13
 **/
public class WindIdempotentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -69962151270633546L;

    public WindIdempotentException(String message) {
        super(message);
    }

    public WindIdempotentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 包装原始异，对于某些场景关注原始异常的，可以识别该异常通过 {@link #getCause()} 获取原始异常
     *
     * @param cause 原始异常
     * @return WindIdempotentException
     */
    public static WindIdempotentException withThrows(Throwable cause) {
        throw new WindIdempotentException("Only wrapper cause throwable", cause);
    }
}
