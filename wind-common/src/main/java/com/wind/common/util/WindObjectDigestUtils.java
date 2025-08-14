package com.wind.common.util;

import com.wind.common.WindConstants;
import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 根据对象生成摘要签名，生成摘要时会根据字典序排序.
 *
 * @author wuxp
 * @date 2024-08-05 16:04
 **/
public final class WindObjectDigestUtils {

    private static final Map<Class<?>, List<String>> CLASS_FIELDS_NAMES = new ConcurrentReferenceHashMap<>();

    private WindObjectDigestUtils() {
        throw new AssertionError();
    }

    /**
     * 根据对象生成摘要
     *
     * @param target 需要生成摘要的对象
     * @return 对象摘要
     */
    public static String sha256(@NotNull Object target) {
        return sha256(target, null);
    }

    /**
     * 根据对象生成摘要
     *
     * @param target 需要生成摘要的对象
     * @param prefix 摘要前缀，可为空
     * @return 对象摘要
     */
    public static String sha256(@NotNull Object target, @Nullable String prefix) {
        return sha256WithNames(target, getObjectFieldNames(target), prefix);
    }

    /**
     * 根据对象指定的字段生成摘要
     *
     * @param target     需要生成摘要的对象
     * @param fieldNames 字段列表
     * @return 对象摘要
     */
    public static String sha256WithNames(@NotNull Object target, @NotEmpty Collection<String> fieldNames) {
        return sha256WithNames(target, fieldNames, null);
    }

    /**
     * 根据对象指定的字段生成摘要
     *
     * @param target     需要生成摘要的对象
     * @param fieldNames 字段列表
     * @param prefix     摘要前缀，可为空
     * @return 对象摘要
     */
    public static String sha256WithNames(@NotNull Object target, @NotEmpty Collection<String> fieldNames, @Nullable String prefix) {
        AssertUtils.notNull(target, "argument target must not null");
        String content = tryGenSha256Text(target);
        if (content != null) {
            return prefix == null ? DigestUtils.sha256Hex(content) : DigestUtils.sha256Hex(prefix + content);
        }
        AssertUtils.notEmpty(fieldNames, "argument fieldNames must not empty");
        return DigestUtils.sha256Hex(genSha256TextWithObject(target, fieldNames, prefix, WindConstants.LF));
    }

    private static String tryGenSha256Text(Object target) {
        if (ClassUtils.isPrimitiveOrWrapper(target.getClass())
                || target.getClass().isArray()
                || target instanceof Number
                || target instanceof CharSequence
                || target instanceof TemporalAccessor
                || target instanceof Collection<?>
                || target instanceof Map<?, ?>) {
            return getValueText(target);
        }
        return null;
    }

    private static List<String> getObjectFieldNames(Object target) {
        AssertUtils.notNull(target, "argument target must not null");
        return CLASS_FIELDS_NAMES.computeIfAbsent(target.getClass(), WindReflectUtils::getFieldNames);
    }

    @VisibleForTesting()
    static String genSha256TextWithObject(Object target, Collection<String> fieldNames, String prefix, String joiner) {
        // TODO 性能优化
        Field[] fields = WindReflectUtils.findFields(target.getClass(), fieldNames);
        Map<String, Field> fieldMaps = Arrays.stream(fields).collect(Collectors.toMap(Field::getName, Function.identity()));
        StringBuilder result = new StringBuilder();
        if (StringUtils.hasText(prefix)) {
            result.append(WindConstants.COLON).append(prefix);
        }
        List<String> sortedNames = fieldNames.stream().sorted().toList();
        for (String name : sortedNames) {
            Field field = fieldMaps.get(name);
            AssertUtils.notNull(field, String.format("field name = %s not found", name));
            try {
                Object val;
                if (field.trySetAccessible()) {
                    val = field.get(target);
                } else {
                    Method method = WindReflectUtils.findFieldGetMethod(field);
                    AssertUtils.notNull(method, () -> String.format("not find get method, field = %s#%s", field.getDeclaringClass().getName(), field.getName()));
                    val = method.invoke(target);
                }
                result.append(name).append(WindConstants.EQ).append(getValueText(val)).append(joiner);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, String.format("get object value error, name = %s", name), exception);
            }
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String getValueText(Object val) {
        if (val == null) {
            return WindConstants.EMPTY;
        }
        if (ClassUtils.isPrimitiveOrWrapper(val.getClass()) || val.getClass().isEnum() || val instanceof CharSequence) {
            // 基础数据类型，枚举，字符串
            return String.valueOf(val);
        } else if (val instanceof Date date) {
            // 获取毫秒数
            return String.valueOf(date.getTime());
        } else if (val instanceof TemporalAccessor temporal) {
            // 获取时间戳
            return String.valueOf(getTemporalAccessorTimestamp(temporal));
        } else if (val.getClass().isArray()) {
            return joinArrayAsText(val);
        } else if (val instanceof Collection<?> collection) {
            return joinCollectionAsText(collection);
        } else if (val instanceof Map<?, ?> map) {
            return joinMapAsText(map);
        } else if (val instanceof Number) {
            // 数值类型
            return String.valueOf(val);
        } else {
            // 对象类型
            return String.format("%s%s%s", WindConstants.DELIM_START, genSha256TextWithObject(val, getObjectFieldNames(val), null, WindConstants.AND), WindConstants.DELIM_END);
        }
    }

    private static String joinMapAsText(Map<?, ?> map) {
        return String.format("%s%s%s", WindConstants.DELIM_START, getMapText(map), WindConstants.DELIM_END);
    }

    private static String joinCollectionAsText(Collection<?> collection) {
        // 集合类型
        return collection.stream()
                .map(WindObjectDigestUtils::getValueText)
                .collect(Collectors.joining(WindConstants.COMMA));
    }

    private static String joinArrayAsText(Object val) {
        if (ClassUtils.isPrimitiveArray(val.getClass())) {
            // 基础数据类型数组
            if (val.getClass().getComponentType() == byte.class) {
                return Arrays.toString((byte[]) val);
            } else if (val.getClass().getComponentType() == short.class) {
                return Arrays.toString((short[]) val);
            } else if (val.getClass().getComponentType() == int.class) {
                return Arrays.toString((int[]) val);
            } else if (val.getClass().getComponentType() == long.class) {
                return Arrays.toString((long[]) val);
            } else if (val.getClass().getComponentType() == float.class) {
                return Arrays.toString((float[]) val);
            } else if (val.getClass().getComponentType() == double.class) {
                return Arrays.toString((double[]) val);
            } else if (val.getClass().getComponentType() == boolean.class) {
                return Arrays.toString((boolean[]) val);
            } else {
                return Arrays.toString((char[]) val);
            }
        } else {
            // 数组
            return Arrays.stream((Object[]) val)
                    .map(WindObjectDigestUtils::getValueText)
                    .collect(Collectors.joining(WindConstants.COMMA));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getMapText(Map map) {
        // 保证 key 的顺序
        Map<Object, Object> sortedKeyParams = new TreeMap<>(map);
        return sortedKeyParams.entrySet()
                .stream()
                .map(entry -> entry.getKey() + WindConstants.EQ + getValueText(entry.getValue()))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(WindConstants.AND));
    }

    private static long getTemporalAccessorTimestamp(TemporalAccessor accessor) {
        switch (accessor) {
            case LocalDateTime localDateTime -> {
                Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
                return instant.toEpochMilli();
            }
            case LocalDate localDate -> {
                Instant instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
                return instant.toEpochMilli();
            }
            case LocalTime localTime -> {
                // 计算从当天零点（午夜）开始到该时间点的 纳秒数
                return localTime.toNanoOfDay();
                // 计算从当天零点（午夜）开始到该时间点的 纳秒数
            }
            default -> {
                return accessor.getLong(ChronoField.MILLI_OF_DAY);
            }
        }
    }
}
