package com.wind.common.executor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.limit.WindExecutionLimiter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
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
     * @value {@link Bucket}
     */
    private static final Cache<@NonNull BucketKey, Bucket> BUCKET_CACHES = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(45))
            .maximumSize(256)
            .build();

    /**
     * Bucket 工厂
     * 参数按顺序为 限流资源名称、限流规则配置
     */
    private static final AtomicReference<BiFunction<String, Bandwidth, Bucket>> BUCKET_FACTORY = new AtomicReference<>((resourceKey, limit) ->
            BUCKET_CACHES.get(new BucketKey(resourceKey, limit), k -> Bucket.builder().addLimit(limit).build()));

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
        return limit(
                resourceKey,
                capacity,
                refillTokens,
                refillPeriod,
                BucketRefillStrategy.GREEDY
        );
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
        return limit(
                resourceKey,
                capacity,
                refillTokens,
                refillPeriod,
                BucketRefillStrategy.INTERVALLY
        );
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
     * @param capacity       桶的容量
     * @param tokenPerSecond 填充的令牌数
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyBucketWithSeconds(String resourceKey, int capacity, int tokenPerSecond) {
        return leakyBucket(resourceKey, capacity, tokenPerSecond, Duration.ofSeconds(1));
    }

    /**
     * 漏桶，匀速填充，不支持累计 （上限为桶的容量 {@param tokenPerSecond}）
     *
     * @param resourceKey    限流的资源或任务唯一标识
     * @param tokenPerSecond 填充的令牌数
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter leakyBucketWithSeconds(String resourceKey, int tokenPerSecond) {
        return leakyBucketWithSeconds(resourceKey, tokenPerSecond, tokenPerSecond);
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
     * 限流
     *
     * @param resourceKey  限流的资源或任务唯一标识
     * @param capacity     桶的容量
     * @param refillTokens 填充的令牌数
     * @param period       填充的间隔时间
     * @param strategy     桶的填充策略
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter limit(String resourceKey, long capacity, long refillTokens, Duration period, BucketRefillStrategy strategy) {
        return limit(resourceKey, capacity, refillTokens, period, strategy, null);
    }

    /**
     * 限流
     *
     * @param resourceKey     限流的资源或任务唯一标识
     * @param capacity        桶的容量
     * @param refillTokens    填充的令牌数
     * @param period          填充的间隔时间
     * @param strategy        桶的填充策略
     * @param firstRefillTime 桶的初始填充时间
     * @return WindExecutionLimiter
     */
    public static WindExecutionLimiter limit(String resourceKey, long capacity, long refillTokens, Duration period, BucketRefillStrategy strategy, Instant firstRefillTime) {
        Bandwidth bandwidth = strategy.build(capacity, refillTokens, period, firstRefillTime);
        return factory(resourceKey, bandwidth);
    }

    /**
     * 设置 Bucket 工厂
     *
     * @param factory Bucket 工厂，参数按顺序为 限流资源名称、限流规则配置
     */
    public static void setBucketFactory(BiFunction<String, Bandwidth, Bucket> factory) {
        BUCKET_FACTORY.set(factory);
    }

    private static WindExecutionLimiter factory(String resourceKey, Bandwidth limit) {
        Bucket bucket = BUCKET_FACTORY.get().apply(resourceKey, limit);
        return (permits, maxWait) -> {
            if (maxWait == null || maxWait.isZero()) {
                return bucket.tryConsume(permits);
            }
            try {
                return bucket.asBlocking().tryConsume(permits, maxWait);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "limit rate task is interrupted, resourceK key = " + resourceKey, exception);
            }
        };
    }


    /**
     * Bucket4j 令牌桶填充策略定义
     *
     * <p>用于控制令牌（tokens）在时间维度上的补充方式，
     * 不同策略会影响限流的“平滑性”、“突发能力”以及“多实例一致性”。</p>
     *
     * <p>核心维度说明：</p>
     * <ul>
     *   <li>refill 方式：是“连续补充”还是“周期性补充”</li>
     *   <li>时间对齐：是否基于固定时间边界（如整秒/整分钟）</li>
     *   <li>初始状态：bucket 启动时是否预填充 tokens</li>
     * </ul>
     */
    public enum BucketRefillStrategy {

        /**
         * 平滑补充（推荐默认策略）
         *
         * <p>令牌按照固定速率持续、均匀地流入桶中（类似水流持续注入）。</p>
         *
         * <p>特点：</p>
         * <ul>
         *   <li>无周期性突刺（no burst refill）</li>
         *   <li>限流行为平滑</li>
         *   <li>更适合 API 调用控制</li>
         * </ul>
         *
         * <p>适用场景：</p>
         * <ul>
         *   <li>高并发接口限流</li>
         *   <li>微服务内部调用保护</li>
         *   <li>需要平滑 QPS 控制的业务</li>
         * </ul>
         */
        GREEDY {

            @Override
            public Bandwidth build(long capacity,
                                   long refillTokens,
                                   @NonNull Duration period,
                                   @Nullable Instant firstRefillTime) {

                return Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(refillTokens, period)
                        .build();
            }
        },

        /**
         * 固定周期补充（非平滑）
         *
         * <p>每个固定时间窗口结束后，一次性补充指定数量 token。</p>
         *
         * <p>行为特征：</p>
         * <ul>
         *   <li>周期性“突发式补充”（burst refill）</li>
         *   <li>窗口内 token 消耗完则阻塞或拒绝</li>
         *   <li>存在典型 fixed-window effect（窗口边界流量突刺）</li>
         * </ul>
         *
         * <p>适用场景：</p>
         * <ul>
         *   <li>简单接口限流</li>
         *   <li>非关键业务保护</li>
         *   <li>允许窗口级统计语义的场景</li>
         * </ul>
         */
        INTERVALLY {

            @Override
            public Bandwidth build(long capacity,
                                   long refillTokens,
                                   @NonNull Duration period,
                                   @Nullable Instant firstRefillTime) {

                return Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(refillTokens, period)
                        .build();
            }
        },

        /**
         * 对齐时间边界的周期补充（分布式一致性优化）
         *
         * <p>在固定周期补充基础上，将 refill 行为对齐到绝对时间边界（如整秒、整分钟）。</p>
         *
         * <p>优势：</p>
         * <ul>
         *   <li>多实例行为一致（K8s / 集群环境推荐）</li>
         *   <li>避免不同节点 refill 时间漂移</li>
         * </ul>
         *
         * <p>行为特征：</p>
         * <ul>
         *   <li>仍然是“周期性突发补充”</li>
         *   <li>但所有节点共享相同时间基准</li>
         * </ul>
         *
         * <p>适用场景：</p>
         * <ul>
         *   <li>API Gateway 全局限流</li>
         *   <li>计费 / 配额系统</li>
         *   <li>多 Pod 一致性要求较高的系统</li>
         * </ul>
         */
        INTERVALLY_ALIGNED {

            @Override
            public Bandwidth build(long capacity,
                                   long refillTokens,
                                   @NonNull Duration period,
                                   @NonNull Instant firstRefillTime) {

                return Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervallyAligned(refillTokens, period, firstRefillTime)
                        .build();
            }
        },

        /**
         * 对齐时间边界 + 自适应初始令牌（冷启动优化）
         *
         * <p>在 ALIGNED 模式基础上，增加“启动时自动补充 token”的能力，
         * 用于避免服务启动初期限流过严的问题。</p>
         *
         * <p>行为特征：</p>
         * <ul>
         *   <li>具备时间对齐能力</li>
         *   <li>启动阶段自动补偿 token（避免冷启动雪崩）</li>
         *   <li>适用于长周期运行服务</li>
         * </ul>
         *
         * <p>适用场景：</p>
         * <ul>
         >
         *   <li>高可用 API 服务</li>
         *   <li>多实例动态扩缩容场景</li>
         *   <li>对冷启动敏感的系统（如支付 / VCC）</li>
         * </ul>
         */
        INTERVALLY_ALIGNED_ADAPTIVE {

            @Override
            public Bandwidth build(long capacity,
                                   long refillTokens,
                                   @NonNull Duration period,
                                   @NonNull Instant firstRefillTime) {

                return Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervallyAlignedWithAdaptiveInitialTokens(
                                refillTokens,
                                period,
                                firstRefillTime
                        )
                        .build();
            }
        };

        /**
         * 构建 Bucket 限流规则
         *
         * @param capacity        桶容量（允许的最大 burst）
         * @param refillTokens    每次补充的 token 数
         * @param period          补充周期
         * @param firstRefillTime 初始对齐时间（仅 ALIGNED 系列生效）
         */
        public abstract Bandwidth build(long capacity,
                                        long refillTokens,
                                        @NonNull Duration period,
                                        Instant firstRefillTime);
    }

    private record BucketKey(String resourceKey, Bandwidth bandwidth) {
    }

}
