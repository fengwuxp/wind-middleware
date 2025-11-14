package com.wind.common.limit;


import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
        return tryAcquire(1);
    }

    /**
     * Acquires a permit from this if it can be acquired immediately without delay.
     *
     * @param permits 获取的许可数量
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryAcquire(int permits) {
        return tryAcquire(permits, Duration.ZERO);
    }

    /**
     * Acquires a permit from this if it can be acquired within the given time limit.
     *
     * @param permits 获取的许可数量
     * @param timeout 最大等待时长
     * @param unit    时长单位
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        return tryAcquire(permits, Duration.of(timeout, unit.toChronoUnit()));
    }

    /**
     * Acquires a permit from this if it can be acquired within the given time limit.
     *
     * @param maxWait 获取许可最大等待时长，为空表示不等待
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    default boolean tryAcquire(Duration maxWait) {
        return tryAcquire(1, maxWait);
    }

    /**
     * Acquires a permit from this if it can be acquired immediately without delay.
     *
     * @param permits 获取的许可数量
     * @param maxWait 最大等待时长，为空表示不等待
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    boolean tryAcquire(int permits, Duration maxWait);



}
