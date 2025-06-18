package com.wind.common.limit;

import com.wind.common.WindConstants;

import jakarta.validation.constraints.Null;
import java.time.Duration;

/**
 * 执行限流器
 *
 * @author wuxp
 * @date 2025-06-09 15:40
 **/
@FunctionalInterface
public interface WindExecutionLimiter {

    default boolean tryAcquire() {
        return tryAcquire(WindConstants.UNKNOWN);
    }

    default boolean tryAcquire(Duration maxWait) {
        return tryAcquire(WindConstants.UNKNOWN, maxWait);
    }

    default boolean tryAcquire(String resourceKey) {
        return tryAcquire(resourceKey, new Object[0]);
    }

    default boolean tryAcquire(String resourceKey, Object... args) {
        return tryAcquire(resourceKey, null, args);
    }

    /**
     * Acquires a permit from this if it can be acquired immediately without delay.
     *
     * @param resourceKey 资源标识
     * @param maxWait     最大等待时长，为空表示不等待
     * @param args        参与限流参数
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    boolean tryAcquire(String resourceKey, @Null Duration maxWait, Object... args);
}
