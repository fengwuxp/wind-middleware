package com.wind.common.executor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * 并发任务装饰器 + 可组合限流（令牌桶 / 漏桶）
 * <p>
 * 该装饰器用于对 Runnable 任务进行并发控制，可选与流量控制（限速）组合使用。
 * 并发控制基于 Semaphore，可自定义全局工厂实现分布式 Semaphore。
 * </p>
 *
 * @author wuxp
 * @date 2025-11-14 11:04
 **/
@Slf4j
public final class ConcurrentTaskDecorator implements TaskDecorator {

    private final String resourceKey;

    private final Duration maxWait;

    private final Semaphore semaphore;

    /**
     * 并发限流器缓存
     *
     * @key 资源标识
     * @value Semaphore
     */
    private static final Cache<@NotNull String, Semaphore> SEMAPHORE_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(256)
            .build();

    /**
     * Semaphore 工厂
     */
    private static final AtomicReference<BiFunction<String, Integer, Semaphore>> SEMAPHORE_FACTORY = new AtomicReference<>((key, maxConcurrent) ->
            SEMAPHORE_CACHE.get(key, k -> new Semaphore(maxConcurrent)));

    private ConcurrentTaskDecorator(String resourceKey, Duration maxWait, int maxConcurrent) {
        this.resourceKey = resourceKey;
        this.maxWait = maxWait;
        this.semaphore = SEMAPHORE_FACTORY.get().apply(resourceKey, maxConcurrent);
    }

    /**
     * 创建并发任务装饰器
     *
     * @param resourceKey   唯一标识
     * @param maxWait       最大等待时间
     * @param maxConcurrent 最大并发数
     * @return 并发任务装饰器
     */
    public static ConcurrentTaskDecorator withConcurrency(@NotBlank String resourceKey, @NotNull Duration maxWait, int maxConcurrent) {
        AssertUtils.notNull(resourceKey, "argument resourceKey must not null");
        AssertUtils.notNull(resourceKey, "argument maxWait must not null");
        AssertUtils.isTrue(maxConcurrent > 0, "argument maxConcurrent must greater than 0");
        return new ConcurrentTaskDecorator(resourceKey, maxWait, maxConcurrent);
    }

    /**
     * 创建并发 & 限流（基于令牌桶）任务装饰器 （优先控制并发数）
     *
     * @param resourceKey    唯一标识
     * @param maxWait        最大等待时间
     * @param maxConcurrent  最大并发数
     * @param tokenPerSecond 限流桶每秒填充数
     * @return 并发任务装饰器
     */
    public static TaskDecorator concurrentWithToken(@NotBlank String resourceKey, @NotNull Duration maxWait, int maxConcurrent, int tokenPerSecond) {
        TaskDecorator concurrent = withConcurrency(resourceKey, maxWait, maxConcurrent);
        TaskDecorator token = RateLimitTaskDecorator.token(resourceKey, tokenPerSecond, tokenPerSecond, maxWait);
        return runnable -> concurrent.decorate(token.decorate(runnable));
    }

    /**
     * 创建并发 & 限流（基于漏桶）任务装饰器（优先控制并发数）
     *
     * @param resourceKey    唯一标识
     * @param maxWait        最大等待时间
     * @param maxConcurrent  最大并发数
     * @param tokenPerSecond 限流桶每秒填充数
     * @return 并发任务装饰器
     */
    public static TaskDecorator concurrentWithLeaky(@NotBlank String resourceKey, @NotNull Duration maxWait, int maxConcurrent, int tokenPerSecond) {
        TaskDecorator concurrent = withConcurrency(resourceKey, maxWait, maxConcurrent);
        TaskDecorator leaky = RateLimitTaskDecorator.leaky(resourceKey, tokenPerSecond, maxWait);
        return runnable -> concurrent.decorate(leaky.decorate(runnable));
    }

    /**
     * 设置自定义工厂（可实现分布式 Semaphore 或其他实现）
     *
     * @param factory 自定义工厂
     */
    public static void setSemaphoreFactory(BiFunction<String, Integer, Semaphore> factory) {
        SEMAPHORE_FACTORY.set(factory);
    }

    @Override
    @NotNull
    public Runnable decorate(@NotNull Runnable runnable) {
        return () -> {
            if (!tryAcquire()) {
                throw BaseException.common("resource key  = %s concurrent limit exceeded".formatted(resourceKey));
            }
            try {
                runnable.run();
            } finally {
                semaphore.release();
            }
        };
    }

    /**
     * 并发限制的方式执行任务
     *
     * @param runnable 执行的函数
     */
    public void execute(@NonNull Runnable runnable) {
        decorate(runnable).run();
    }

    private boolean tryAcquire() {
        if (maxWait.isZero()) {
            return semaphore.tryAcquire();
        }
        try {
            return semaphore.tryAcquire(1, maxWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Concurrent limit interrupted for resourceKey = {}", resourceKey, exception);
            return false;
        }
    }

}
