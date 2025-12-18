package com.wind.common.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 异常工具类
 *
 * @author wuxp
 * @date 2025-12-18 16:28
 **/
public final class WindThrowableUtils {

    private WindThrowableUtils() {
        throw new AssertionError();
    }

    /**
     * 判断异常 cause 是否是指定类型的异常
     *
     * @param throwable  待判断的异常
     * @param causeClass cause 的类
     * @return true: 是指定类型的异常
     */
    public static boolean isCausedBy(@Nullable Throwable throwable, @NonNull Class<? extends Throwable> causeClass) {
        while (throwable != null) {
            if (causeClass.isInstance(throwable)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
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
        while (current.getCause() != null) {
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
    public static Throwable findCauseOfType(@NonNull Throwable throwable, @NonNull Class<? extends Throwable> causeClass) {
        while (throwable != null) {
            if (causeClass.isInstance(throwable)) {
                return throwable;
            }
            throwable = throwable.getCause();
        }
        return null;
    }
}
