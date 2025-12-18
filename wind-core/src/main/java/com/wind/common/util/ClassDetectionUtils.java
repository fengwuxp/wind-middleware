package com.wind.common.util;

/**
 * 类探测工具
 *
 * @author wuxp
 * @date 2023-10-19 15:48
 **/
@Deprecated(forRemoval = true)
public final class ClassDetectionUtils {

    private ClassDetectionUtils() {
        throw new AssertionError();
    }

    /**
     * 类是否存在
     *
     * @param className 类名
     * @return 是否存在
     */
    public static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
