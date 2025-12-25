package com.wind.sequence.time;

import com.wind.sequence.NumericSequenceGenerator;
import com.wind.sequence.SequenceGenerator;
import com.wind.sequence.WindSequenceType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * 时间前缀序列号生成工厂（支持秒/分钟/小时/天/月/年粒度）
 * 可注入自定义计数器策略
 * 线程安全，但默认为单节点内存计数器，如需分布式请注入自定义计数器
 *
 * @author wuxp
 * @date 2025-12-25 13:15
 **/
public final class TemporalSequenceFactory {

    /**
     * 计数器提供者，可替换为分布式实现
     */
    private static final AtomicReference<BiFunction<SequenceTimeScopeType, WindSequenceType, Long>> COUNTER = new AtomicReference<>(new DefaultCounterSupplier());

    private TemporalSequenceFactory() {
        throw new AssertionError();
    }

    /**
     * 获取下一个时间序列号生成器
     *
     * @param sequenceType 序列号类型
     * @return 序列号生成器
     */
    @NotNull
    public static SequenceGenerator second(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.SECONDS, sequenceType);
    }

    @NotNull
    public static SequenceGenerator minute(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.MINUTE, sequenceType);
    }

    @NotNull
    public static SequenceGenerator hour(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.HOUR, sequenceType);
    }

    @NotNull
    public static SequenceGenerator day(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.DAY, sequenceType);
    }

    @NotNull
    public static SequenceGenerator month(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.MONTH, sequenceType);
    }

    @NotNull
    public static SequenceGenerator year(WindSequenceType sequenceType) {
        return wrap(SequenceTimeScopeType.YEAR, sequenceType);
    }

    /**
     * 获取下一个时间序列号
     *
     * @param sequenceType 序列号类型
     * @return 时间序列号
     */
    @NotNull
    public static String secondNext(@NotNull WindSequenceType sequenceType) {
        return second(sequenceType).next();
    }

    @NotNull
    public static String minuteNext(@NotNull WindSequenceType sequenceType) {
        return minute(sequenceType).next();
    }

    @NotNull
    public static String hourNext(@NotNull WindSequenceType sequenceType) {
        return hour(sequenceType).next();
    }

    @NotNull
    public static String dayNext(@NotNull WindSequenceType sequenceType) {
        return day(sequenceType).next();
    }

    @NotNull
    public static String monthNext(@NotNull WindSequenceType sequenceType) {
        return month(sequenceType).next();
    }

    @NotNull
    public static String yearNext(@NotNull WindSequenceType sequenceType) {
        return year(sequenceType).next();
    }

    /**
     * 获取下一个时间序列号
     *
     * @param scope        时间范围
     * @param sequenceType 序列号类型
     * @return 时间序列号
     */
    @NotNull
    public static String next(SequenceTimeScopeType scope, WindSequenceType sequenceType) {
        return wrap(scope, sequenceType).next();
    }

    private static SequenceGenerator wrap(SequenceTimeScopeType scope, WindSequenceType sequenceType) {
        NumericSequenceGenerator generator = new NumericSequenceGenerator(() -> COUNTER.get().apply(scope, sequenceType), sequenceType.length());
        SequenceGenerator delegate = DateTimeSequenceGenerator.of(scope, generator);
        return () -> sequenceType.getPrefix() + delegate.next();
    }

    /**
     * 注入自定义计数器策略
     */
    public static void setCounter(@NotNull BiFunction<SequenceTimeScopeType, WindSequenceType, Long> counter) {
        COUNTER.set(counter);
    }


    // -------------------- 默认内存计数器 --------------------
    private static class DefaultCounterSupplier implements BiFunction<SequenceTimeScopeType, WindSequenceType, Long> {
        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

        @Override
        public Long apply(SequenceTimeScopeType scopeType, WindSequenceType sequenceType) {
            // 使用 scopeType + sequenceType 做唯一 key，避免冲突
            String key = scopeType.name() + "@" + sequenceType.name();
            return counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
}
