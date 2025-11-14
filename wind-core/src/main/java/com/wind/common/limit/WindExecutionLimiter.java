package com.wind.common.limit;


import java.time.Duration;

/**
 * 执行限流器
 *
 * @author wuxp
 * @date 2025-06-09 15:40
 **/
@FunctionalInterface
public interface WindExecutionLimiter {

    /**
     * Acquires a permit from this if it can be acquired immediately without delay.
     *
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryAcquire() {
        return tryAcquire(Duration.ZERO);
    }

    /**
     * Acquires a permit from this if it can be acquired immediately without delay.
     *
     * @param maxWait 最大等待时长，为空表示不等待
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    boolean tryAcquire(Duration maxWait);

}
