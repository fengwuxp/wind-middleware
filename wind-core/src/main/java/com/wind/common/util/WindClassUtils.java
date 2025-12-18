package com.wind.common.util;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 类工具类
 *
 * @author wuxp
 * @date 2025-12-18 16:13
 **/
public final class WindClassUtils {

    private WindClassUtils() {
        throw new AssertionError();
    }

    /**
     * 类是否存在
     *
     * @param className 类名
     * @return 是否存在
     */
    public static boolean isPresent(@NonNull String className) {
        return loadClass(className) != null;
    }

    /**
     * 尝试加载类
     *
     * @param className 类名
     * @return 类
     */
    @Nullable
    public static Class<?> loadClass(@NonNull String className) {
        return loadClass(className, WindClassUtils.class.getClassLoader());
    }

    /**
     * 尝试加载类
     *
     * @param className   类名
     * @param classLoader 类加载器
     * @return 类
     */
    @Nullable
    public static Class<?> loadClass(String className, @Nullable ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 加载类，如果加载失败则抛异常
     *
     * @param className 类名
     * @return 类
     */
    @NonNull
    public static Class<?> requireClass(@NonNull String className) {
        return requireClass(className, WindClassUtils.class.getClassLoader());
    }

    /**
     * 加载类，如果加载失败则抛异常
     *
     * @param className   类名
     * @param classLoader 类加载器
     * @return 类
     */
    public static Class<?> requireClass(@NonNull String className, @Nullable ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "class not found: " + className, e);
        }
    }



}
