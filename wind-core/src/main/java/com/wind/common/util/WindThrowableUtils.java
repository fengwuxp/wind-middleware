package com.wind.common.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 异常工具类
 *
 * @author wuxp
 * @date 2025-12-18 16:28
 **/
public final class WindThrowableUtils {

    private WindThrowableUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * 判断异常 cause 是否是指定类型的异常
     *
     * @param throwable  待判断的异常
     * @param causeClass cause 的类
     * @return true: 是指定类型的异常
     */
    public static boolean isCausedBy(@Nullable Throwable throwable, @NonNull Class<? extends Throwable> causeClass) {
        return findCauseOfType(throwable, causeClass) != null;
    }

    /**
     * 获取异常 cause 的根异常
     *
     * @param throwable 待判断的异常
     * @return throwable 的根异常
     */
    @NonNull
    public static Throwable getRootCause(@NonNull Throwable throwable) {
        Throwable current = throwable;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(current);

        while (current.getCause() != null && visited.add(current.getCause())) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 在异常链中查找指定类型的异常（包含当前异常本身）
     *
     * @param throwable  起始异常
     * @param causeClass 目标异常类型
     * @return 链中第一个匹配的异常，如果不存在则返回 null
     */
    @Nullable
    public static <T extends Throwable> T findCauseOfType(@Nullable Throwable throwable, @NonNull Class<T> causeClass) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        while (throwable != null && visited.add(throwable)) {
            if (causeClass.isInstance(throwable)) {
                return causeClass.cast(throwable);
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    /**
     * 判断异常或异常 cause 的 message 是否包含指定字符串
     *
     * @param throwable       待判断的异常
     * @param expectedMessage 预期的消息
     * @return true: 包含
     */
    public static boolean containsExceptionMessage(@Nullable Throwable throwable, @NonNull String expectedMessage) {
        if (expectedMessage.isBlank()) {
            return false;
        }

        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        while (throwable != null && visited.add(throwable)) {
            String throwableMessage = throwable.getMessage();
            if (throwableMessage != null && throwableMessage.contains(expectedMessage)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }
}