package com.wind.middleware.idempotent;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.WindIdempotentException;
import com.wind.common.function.WindFunctions;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 幂等服务执行包装工具
 *
 * @author wuxp
 * @date 2025-10-13 10:30
 **/
@Slf4j
public final class WindIdempotentExecuteUtils {

    private static final AtomicReference<WindIdempotentKeyStorage> STORAGE = new AtomicReference<>();

    private WindIdempotentExecuteUtils() {
        throw new AssertionError();
    }

    public static <T> T execute(String idempotentKey, Supplier<T> supplier) {
        return executeWithThrows(idempotentKey, supplier::get, v -> {
        });
    }

    public static <T> T executeWithThrows(String idempotentKey, WindFunctions.ThrowsSupplier<T> supplier) {
        return executeWithThrows(idempotentKey, supplier, v -> {
        });
    }

    /**
     * 执行一个幂等任务。
     * 如果同一 idempotentKey 已经执行过，则直接返回历史结果；
     * 否则执行 supplier，并缓存结果。
     *
     * @param idempotentKey          幂等 key
     * @param supplier               幂等任务
     * @param onIdempotentedCallback 幂等任务处于幂等状态的的回调
     */
    public static <T> T executeWithThrows(String idempotentKey, WindFunctions.ThrowsSupplier<T> supplier, Consumer<T> onIdempotentedCallback) {
        WindIdempotentKeyStorage storage = requireStorage();
        WindIdempotentValueWrapper wrapper = storage.checkExistsAndGetValue(idempotentKey);
        if (wrapper == null) {
            // TODO 限制并发？
            try {
                T value = supplier.get();
                // 保存幂等执行结果
                storage.save(idempotentKey, value);
                return value;
            } catch (Throwable e) {
                throw WindIdempotentException.withThrows(e);
            }
        }
        T result = wrapper.getValue();
        onIdempotentedCallback.accept(result);
        log.info("idempotentKey = {} 服务已执行过, 从执行记录中返回结果", idempotentKey);
        return result;
    }

    public static void execute(String idempotentKey, Runnable runnable) {
        executeWithThrows(idempotentKey, runnable::run, () -> {
        });
    }

    public static void executeWithThrows(String idempotentKey, WindFunctions.ThrowsRunnable runnable) {
        executeWithThrows(idempotentKey, runnable, () -> {
        });
    }

    /**
     * @param idempotentKey       幂等 key
     * @param runnable            幂等任务
     * @param runWithIdempotented 幂等任务处于幂等状态的的回调
     */
    public static void executeWithThrows(String idempotentKey, WindFunctions.ThrowsRunnable runnable, Runnable runWithIdempotented) {
        WindIdempotentKeyStorage storage = requireStorage();
        if (storage.exists(idempotentKey)) {
            runWithIdempotented.run();
            log.info("idempotentKey = {} 服务已执行过", idempotentKey);
            return;
        }
        try {
            runnable.run();
        } catch (Throwable e) {
            throw WindIdempotentException.withThrows(e);
        }
        storage.save(idempotentKey, null);
    }

    public static void configureStorage(WindIdempotentKeyStorage storage) {
        STORAGE.set(storage);
    }

    private static WindIdempotentKeyStorage requireStorage() {
        WindIdempotentKeyStorage result = STORAGE.get();
        AssertUtils.notNull(result, "WindIdempotentKeyStorage uninitialized");
        return result;
    }
}
