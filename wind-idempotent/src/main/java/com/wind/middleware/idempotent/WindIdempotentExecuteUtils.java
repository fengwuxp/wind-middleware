package com.wind.middleware.idempotent;

import com.wind.common.exception.AssertUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 幂等服务执行包装工具
 *
 * @author wuxp
 * @date 2025-10-13 10:30
 **/
public final class WindIdempotentExecuteUtils {

    private static final AtomicReference<WindIdempotentKeyStorage> STORAGE = new AtomicReference<>();

    private WindIdempotentExecuteUtils() {
        throw new AssertionError();
    }

    /**
     * 执行一个幂等任务。
     * 如果同一 idempotentKey 已经执行过，则直接返回历史结果；
     * 否则执行 supplier，并缓存结果。
     */
    public static <T> T execute(String idempotentKey, Supplier<T> supplier) {
        WindIdempotentKeyStorage storage = requireStorage();
        WindIdempotentValueWrapper wrapper = storage.checkExistsAndGetValue(idempotentKey);
        if (wrapper == null) {
            // TODO 限制并发？
            T value = supplier.get();
            // 保存幂等执行结果
            storage.save(idempotentKey, value);
            return value;
        }
        return wrapper.getValue();
    }

    private static WindIdempotentKeyStorage requireStorage() {
        WindIdempotentKeyStorage result = STORAGE.get();
        AssertUtils.notNull(result, "WindIdempotentKeyStorage uninitialized");
        return result;
    }

    /**
     * 执行一个幂等任务。
     */
    public static void execute(String idempotentKey, Runnable runnable) {
        WindIdempotentKeyStorage storage = requireStorage();
        if (storage.exists(idempotentKey)) {
            return;
        }
        runnable.run();
        storage.save(idempotentKey, null);
    }

    public static void configureStorage(WindIdempotentKeyStorage storage) {
        STORAGE.set(storage);
    }
}
