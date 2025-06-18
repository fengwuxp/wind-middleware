package com.wind.common.util;

import com.wind.common.exception.AssertUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;

/**
 * 委托或装饰器模式下用于查找被委托或代理的对象
 *
 * @author wuxp
 * @date 2025-04-07 09:50
 **/
public final class DelegateObjectUtils {

    /**
     * 默认查找委托对象的 get 方法名称
     */
    private static final String DEFAULT_GET_METHOD_NAME = "getDelegate";

    private DelegateObjectUtils() {
        throw new AssertionError();
    }

    /**
     * 查找被委托或代理的对象
     *
     * @param delegator   委托者对象
     * @param targetClass 目标类
     * @return 目标类对象
     */
    @NotNull
    public static <T> T requireDelegate(Object delegator, Class<T> targetClass) {
        return requireDelegate(delegator, DEFAULT_GET_METHOD_NAME, targetClass);
    }

    /**
     * 查找被委托或代理的对象
     *
     * @param delegator             委托者对象
     * @param getDelegateMethodName 获取委托对象的方法名称
     * @param targetClass           目标类
     * @return 目标类对象
     */
    @NotNull
    public static <T> T requireDelegate(Object delegator, String getDelegateMethodName, Class<T> targetClass) {
        T result = findDelegate(delegator, getDelegateMethodName, targetClass);
        AssertUtils.notNull(result, "not found targetClass = " + targetClass.getName() + " delegate object");
        return result;
    }


    /**
     * 查找被委托或代理的对象
     *
     * @param delegator   委托者对象
     * @param targetClass 目标类
     * @return 目标类对象
     */
    public static <T> T findDelegate(Object delegator, Class<T> targetClass) {
        return findDelegate(delegator, DEFAULT_GET_METHOD_NAME, targetClass);
    }

    /**
     * 查找被委托或代理的对象
     *
     * @param delegator             委托者对象
     * @param getDelegateMethodName 获取委托对象的方法名称
     * @param targetClass           目标类
     * @return 目标类对象
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T findDelegate(Object delegator, String getDelegateMethodName, Class<T> targetClass) {
        if (delegator == null) {
            return null;
        }
        if (delegator.getClass() == targetClass) {
            return (T) delegator;
        }
        Method getMethod = ReflectionUtils.findMethod(delegator.getClass(), getDelegateMethodName);
        AssertUtils.notNull(getMethod,
                "not found getDelegateMethodName = " + getDelegateMethodName + ", delegator class = " + delegator.getClass().getName());
        return findDelegate(ReflectionUtils.invokeMethod(getMethod, delegator), getDelegateMethodName, targetClass);
    }
}
