package com.wind.common.executor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.limit.WindExecutionLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * 基于 <a href="https://github.com/bucket4j/bucket4j">bucket4j</a> 实现的令牌桶/漏桶 任务限流器
 *
 * @author wuxp
 * @date 2025-06-09 11:03
 **/
@Slf4j
public final class Bucket4jTaskExecutionLimiterFactory {

    /**
     * 本地限流缓存器
     *
     * @key 需要限流的资源名称
     * @value 限流器
     */
    private static final Cache<@org.jetbrains.annotations.NotNull String, Bucket> BUCKET_CACHES = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(3))
            .maximumSize(256)
            .build();

    static final AtomicReference<BiFunction<String, Bandwidth, Bucket>> BUCKET_FACTORY = new AtomicReference<>((resourceKey, limit) ->
            BUCKET_CACHES.get(resourceKey, k -> Bucket.builder().addLimit(limit).build()));

    private Bucket4jTaskExecutionLimiterFactory() {
        throw new AssertionError();
    }

    /**
     * 令牌桶，支持累计（（上限为桶的容量 {@param capacity}）
     *
     * @param resourceKey  限流的资源或任务唯一标识
     * @param capacity     桶的容量
     * @param refillTokens 填充的令牌数
     * @param refillPeriod 填充的间隔时间
     * @return ExecuteTaskRateLimiter
     */
    public static WindExecutionLimiter tokenBucket(String resourceKey, int capacity, int refillTokens, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillPeriod)
                .build();
        return factory(resourceKey, limit);
    }

    /**
     * 令牌桶，支持累计（上限为桶的容量 {@param capacity}）
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param capacity       桶的容量
     * @param tokenPerSecond 填充的令牌数
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter tokenBucket(String resourceKey, int capacity, int tokenPerSecond) {
        return tokenBucket(resourceKey, capacity, tokenPerSecond, Duration.ofSeconds(1));
    }

    /**
     * 令牌桶，支持累计（上限为桶的容量 {@param tokenPerSecond}）
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param tokenPerSecond 填充的令牌数
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter tokenWithSingleSeconds(String resourceKey, int tokenPerSecond) {
        return tokenBucket(resourceKey, tokenPerSecond, tokenPerSecond);
    }

    /**
     * 令牌桶，支持累计（上限为桶的容量 1）
     *
     * @param resourceKey 限流的资源或任务唯一标识
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter tokenWithSingleSeconds(String resourceKey) {
        return tokenBucket(resourceKey, 1, 1);
    }

    /**
     * 漏桶，匀速填充，不支持累计 （上限为桶的容量 {@param capacity}）
     *
     * @param resourceKey  限流的资源或任务唯一标识
     * @param capacity     桶的容量
     * @param refillTokens 填充的令牌数
     * @param refillPeriod 填充的间隔时间
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyBucket(String resourceKey, int capacity, int refillTokens, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, refillPeriod)
                .build();
        return factory(resourceKey, limit);
    }

    /**
     * 漏桶，匀速填充，不支持累计 （上限为桶的容量 {@param refillTokens}）
     *
     * @param resourceKey  限流的资源或任务唯一标识
     * @param refillTokens 填充的令牌数
     * @param refillPeriod 填充的间隔时间
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyBucket(String resourceKey, int refillTokens, Duration refillPeriod) {
        return leakyBucket(resourceKey, refillTokens, refillTokens, refillPeriod);
    }

    /**
     * 漏桶，匀速填充，不支持累计 （上限为桶的容量 {@param tokenPerSecond}）
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param tokenPerSecond 填充的令牌数
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyBucketWithSeconds(String resourceKey, int tokenPerSecond) {
        return leakyBucket(resourceKey, tokenPerSecond, tokenPerSecond, Duration.ofSeconds(1));
    }

    /**
     * 漏桶，匀速填充，不支持累计 （上限为桶的容量 1）
     *
     * @param resourceKey 限流的资源或任务唯一标识
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyWithSingleSeconds(String resourceKey) {
        return leakyBucketWithSeconds(resourceKey, 1);
    }

    /**
     * 设置 Bucket 工厂
     *
     * @param factory Bucket 工厂
     */
    public static void setBucketFactory(BiFunction<String, Bandwidth, Bucket> factory) {
        BUCKET_FACTORY.set(factory);
    }

    private static WindExecutionLimiter factory(String resourceKey, Bandwidth limit) {
        Bucket bucket = BUCKET_FACTORY.get().apply(resourceKey, limit);
        return maxWait -> {
            if (maxWait == null || maxWait.isZero()) {
                return bucket.tryConsume(1);
            } else {
                try {
                    return bucket.asBlocking().tryConsume(1, maxWait);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "limit rate task is interrupted, resourceK key = " + resourceKey, exception);
                }
            }
        };
    }

}
