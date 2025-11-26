package com.wind.common.executor;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.limit.WindExecutionLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 限流任务装饰器
 *
 * @author wuxp
 * @date 2025-06-10 09:48
 **/
@Slf4j
public record RateLimiterTaskDecorator(String resourceKey, WindExecutionLimiter limiter, Duration maxWait) implements TaskDecorator {

    /**
     * 是否抛出限流异常
     * 默认 false 仅输出告警日志
     */
    private static final AtomicBoolean THROW_EXCEPTION_WITH_LIMIT = new AtomicBoolean(false);

    /**
     * 创建限流任务装饰器（匀速执行），默认等待时间 200 毫秒
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param tokenPerSecond 每秒执行数
     * @param maxWait        最大等待时间
     * @return 限流任务装饰器
     */
    public static RateLimiterTaskDecorator leaky(String resourceKey, int tokenPerSecond, Duration maxWait) {
        return new RateLimiterTaskDecorator(resourceKey, Bucket4jTaskExecutionLimiterFactory.leakyBucketWithSeconds(resourceKey, tokenPerSecond), maxWait);
    }

    public static RateLimiterTaskDecorator leaky(String taskName, int tokenPerSecond) {
        return leaky(taskName, tokenPerSecond, Duration.ofMillis(500));
    }

    /**
     * 创建限流任务装饰器（令牌桶），默认等待时间 200 毫秒
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param capacity       容量
     * @param tokenPerSecond token 每秒填充数
     * @param maxWait        最大等待时间
     * @return 限流任务装饰器
     */
    public static RateLimiterTaskDecorator token(String resourceKey, int capacity, int tokenPerSecond, Duration maxWait) {
        return new RateLimiterTaskDecorator(resourceKey, Bucket4jTaskExecutionLimiterFactory.tokenBucket(resourceKey, capacity, tokenPerSecond), maxWait);
    }

    public static RateLimiterTaskDecorator token(String taskName, int capacity, int tokenPerSecond) {
        return token(taskName, capacity, tokenPerSecond, Duration.ofMillis(500));
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        return () -> {
            if (limiter.tryAcquire(maxWait)) {
                log.debug("execute rate limit decorate task, resource key = {}", resourceKey);
                runnable.run();
            } else {
                if (THROW_EXCEPTION_WITH_LIMIT.get()) {
                    throw new BaseException(DefaultExceptionCode.TO_MANY_REQUESTS, "resource key  = %s rate limit exceeded".formatted(resourceKey));
                } else {
                    // 仅打印警告
                    log.warn("task name  = {} rate limit exceeded", resourceKey);
                }
            }
        };
    }

    /**
     * 以限流的方式执行任务
     *
     * @param runnable 执行的函数
     */
    public void execute(@NonNull Runnable runnable) {
        decorate(runnable).run();
    }

    public static void setThrowExceptionWithLimit(boolean throwExceptionWithLimit) {
        THROW_EXCEPTION_WITH_LIMIT.set(throwExceptionWithLimit);
    }
}
