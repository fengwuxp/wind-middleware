package com.wind.common.util;

import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * wind 类型解析工具
 *
 * @author wuxp
 * @date 2025-09-30 13:26
 **/
public final class WindTypeResolveUtils {

    private WindTypeResolveUtils() {
        throw new AssertionError();
    }

    /**
     * 解析对象继承的超类或实现的接口上设置的泛型
     *
     * @param bean        对象
     * @param supperClass 指定的接口或超类，如果不为空，则只获取指定超类或接口上的泛型
     * @return 父类或者接口上设置的泛型
     */
    public static Type[] resolveSuperGenericType(@NotNull Object bean, @Nullable Class<?> supperClass) {
        AssertUtils.notNull(bean, "argument bean must not null");
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        ResolvableType matchType;
        if (supperClass != null) {
            matchType = findGenericTypeRecursive(ResolvableType.forClass(targetClass), supperClass);
            if (matchType == null) {
                throw new IllegalArgumentException(targetClass.getName() + " 未实现或继承 " + supperClass.getName());
            }
        } else {
            // 未指定 supperClass，优先找直接父类，如果父类是 Object 就取第一个接口
            ResolvableType resolvableType = ResolvableType.forClass(targetClass);
            ResolvableType superType = resolvableType.getSuperType();
            if (superType != ResolvableType.NONE && superType.getRawClass() != Object.class) {
                matchType = superType;
            } else {
                ResolvableType[] interfaces = resolvableType.getInterfaces();
                matchType = (interfaces.length > 0) ? interfaces[0] : null;
            }
        }

        if (matchType == null) {
            return new Type[0];
        }

        ResolvableType[] generics = matchType.getGenerics();
        AssertUtils.notEmpty(generics, () -> targetClass.getName() + " 未设置泛型");
        return Arrays.stream(generics)
                .map(ResolvableType::getType)
                .toArray(Type[]::new);
    }

    /**
     * 解析对象继承的超类或实现的接口上设置的所有泛型
     *
     * @param bean 对象
     * @return 父类或者接口上设置的所有泛型
     */
    public static Type[] resolveSuperGenericType(@NotNull Object bean) {
        return resolveSuperGenericType(bean, null);
    }

    /**
     * 递归查找继承链/接口树上指定 supperClass 对应的 ResolvableType
     */
    @Nullable
    private static ResolvableType findGenericTypeRecursive(ResolvableType type, Class<?> supperClass) {
        if (type == ResolvableType.NONE) {
            return null;
        }
        Class<?> rawClass = type.getRawClass();
        if (supperClass.equals(rawClass)) {
            return type;
        }
        ResolvableType superType = type.getSuperType();
        ResolvableType result = findGenericTypeRecursive(superType, supperClass);
        if (result != null) {
            return result;
        }
        for (ResolvableType iface : type.getInterfaces()) {
            result = findGenericTypeRecursive(iface, supperClass);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
