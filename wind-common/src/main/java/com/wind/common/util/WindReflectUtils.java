package com.wind.common.util;

import com.wind.common.exception.AssertUtils;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反射工具类，注意不支持静态字段
 *
 * @author wuxp
 * @date 2024-08-02 14:57
 **/
public final class WindReflectUtils {

    private static final Field[] EMPTY = new Field[0];

    private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentReferenceHashMap<>();

    /**
     * 根据注解查找 {@link Field}，会递归查找超类
     *
     * @param clazz           类类型
     * @param annotationClass 注解类型
     * @return 字段列表
     */
    @NotNull
    public static Field[] findFields(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        AssertUtils.notNull(annotationClass, "argument annotationClass must not null");
        Field[] result = getMemberFields(clazz)
                .stream()
                .filter(field -> field.isAnnotationPresent(annotationClass))
                .toArray(Field[]::new);
        trySetAccessible(result);
        return result;
    }

    /**
     * 根据字段名称查找 {@link Field}，会递归查找超类
     *
     * @param clazz      类类型
     * @param fieldNames 字段名称集合
     * @return 字段列表
     */
    @NotNull
    public static Field[] findFields(Class<?> clazz, Collection<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return EMPTY;
        }
        AssertUtils.notNull(clazz, "argument clazz must not null");
        Set<String> names = new HashSet<>(fieldNames);
        Field[] result = getMemberFields(clazz)
                .stream()
                .filter(field -> names.contains(field.getName()))
                .distinct()
                .toArray(Field[]::new);
        trySetAccessible(result);
        return result;
    }

    /**
     * 根据字段名称查找 {@link Field}，会递归查找超类
     *
     * @param clazz     类类型
     * @param fieldName 字段名称
     * @return 字段
     */
    @NotNull
    public static Field findField(Class<?> clazz, String fieldName) {
        Field result = findFieldNullable(clazz, fieldName);
        AssertUtils.notNull(result, String.format("not found name = %s field", fieldName));
        return result;
    }

    /**
     * 根据字段名称查找 {@link Field}，会递归查找超类
     *
     * @param clazz     类类型
     * @param fieldName 字段名称
     * @return 字段
     */
    @Null
    public static Field findFieldNullable(Class<?> clazz, String fieldName) {
        Field[] fields = findFields(clazz, Collections.singleton(fieldName));
        if (fields.length == 0) {
            return null;
        }
        return fields[0];
    }

    @NotNull
    public static Field[] getFields(Class<?> clazz) {
        return findFields(clazz, getFieldNames(clazz));
    }

    /**
     * 获取类的所有字段名称
     *
     * @param clazz 类类型
     * @return 字段名称列表
     */
    public static List<String> getFieldNames(Class<?> clazz) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return getMemberFields(clazz)
                .stream()
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取类的 方法列表
     *
     * @param clazz 类类型
     * @return 方法列表
     */
    public static Method[] getMethods(Class<?> clazz) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return clazz.getMethods();
    }

    /**
     * 获取字段的 getter 方法
     *
     * @param field 字段
     * @return get 方法
     */
    public static Method findFieldGetMethod(Field field) {
        Class<?> clazz = field.getDeclaringClass();
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        // 构建 getter 方法名
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String getterName = "get" + capitalized;
        try {
            return clazz.getMethod(getterName); // 只找 public 方法
        } catch (NoSuchMethodException e) {
            // fallback：尝试找 getXxx 形式（boolean 时 isXxx 可能找不到）
            if (fieldType == boolean.class || fieldType == Boolean.class) {
                try {
                    return clazz.getMethod("is" + capitalized);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
            return null;
        }
    }


    /**
     * 获取类的公有 getter 方法列表
     *
     * @param clazz 类类型
     * @return 方法列表
     */
    public static Method[] getGetterMethods(Class<?> clazz) {
        AssertUtils.notNull(clazz, "argument clazz must not null");
        return Arrays.stream(getMethods(clazz))
                .filter(WindReflectUtils::isGetterMethod)
                .toArray(Method[]::new);
    }

    private static boolean isGetterMethod(Method method) {
        String name = method.getName();
        if (Objects.equals(name, "getClass")) {
            // getClass 是 object 中的方法，忽略
            return false;
        }
        // 1. 方法名必须以 "get" 开头，长度 > 3，或以 "is" 开头且长度 > 2
        boolean isGet = name.startsWith("get") && name.length() > 3;
        boolean isBooleanGet = name.startsWith("is") && name.length() > 2;
        // 2. 方法必须没有参数
        boolean noParams = method.getParameterCount() == 0;
        // 3. 方法返回类型不能是 void
        boolean hasReturn = method.getReturnType() != void.class;
        // 4. 非静态方法
        boolean isNotStatic = !Modifier.isStatic(method.getModifiers());
        // 5. 公有方法
        boolean isPublic = Modifier.isPublic(method.getModifiers());
        // 综合判断
        return isPublic && isNotStatic && noParams && hasReturn && (isGet || isBooleanGet);
    }

    /**
     * 获取成员变量
     *
     * @param clazz 类类型
     * @return 字段列表
     */
    private static List<Field> getMemberFields(Class<?> clazz) {
        return FIELDS.computeIfAbsent(clazz, WindReflectUtils::getClazzFields)
                .stream()
                // 过滤静态变量
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toList());
    }

    private static List<Field> getClazzFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return Collections.emptyList();
        }
        Field[] fields = clazz.getDeclaredFields();
        List<Field> result = new ArrayList<>(Arrays.asList(fields));
        result.addAll(getClazzFields(clazz.getSuperclass()));
        return result;
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
     * 解析对象继承的超类或实现的接口上设置的泛型
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
        if (rawClass != null && supperClass.equals(rawClass)) {
            return type;
        }

        // 先递归查找父类
        ResolvableType superType = type.getSuperType();
        ResolvableType result = findGenericTypeRecursive(superType, supperClass);
        if (result != null) {
            return result;
        }

        // 再递归查找接口
        for (ResolvableType iface : type.getInterfaces()) {
            result = findGenericTypeRecursive(iface, supperClass);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void trySetAccessible(Field[] fields) {
        for (Field field : fields) {
            field.trySetAccessible();
        }
    }
}
