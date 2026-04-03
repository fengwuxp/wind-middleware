package com.wind.common.util;

import com.wind.core.ReadonlyContextVariables;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 上下文变量工具类
 *
 * @author wuxp
 * @date 2026-04-03 11:08
 **/
public final class WindContextVariablesUtils {

    private static final AtomicReference<JsonMapper> MAPPER = new AtomicReference<>(new JsonMapper());

    private WindContextVariablesUtils() {
        throw new AssertionError();
    }

    /**
     * 获取变量
     *
     * @param context 上下文
     * @param key     key
     * @param type    类型
     * @param <T>     泛型
     * @return 变量
     */
    public static <T> T asVariable(@NonNull ReadonlyContextVariables context, @NonNull String key, @NonNull ParameterizedTypeReference<T> type) {
        return convertValue(() -> context.getContextVariable(key), type.getType());
    }

    public static <T> T asVariable(@NonNull ReadonlyContextVariables context, @NonNull String key, @NonNull TypeReference<T> type) {
        return convertValue(() -> context.getContextVariable(key), type.getType());
    }

    public static <T> T asVariable(@NonNull ReadonlyContextVariables context, @NonNull String key, @NonNull Class<T> type) {
        return convertValue(() -> context.getContextVariable(key), type);
    }

    public static <T> T asVariable(@NonNull Map<String, Object> context, @NonNull String key, @NonNull TypeReference<T> type) {
        return convertValue(() -> context.get(key), type.getType());
    }

    public static <T> T asVariable(@NonNull Map<String, Object> context, @NonNull String key, @NonNull ParameterizedTypeReference<T> type) {
        return convertValue(() -> context.get(key), type.getType());
    }

    public static <T> T asVariable(@NonNull Map<String, Object> context, @NonNull String key, @NonNull Class<T> type) {
        return convertValue(() -> context.get(key), type);
    }

    private static <T> T convertValue(Supplier<Object> variableGetter, Type type) {
        Object variable = variableGetter.get();
        if (variable == null) {
            return null;
        }
        JsonMapper jsonMapper = MAPPER.get();
        // 根据 type 构造 Jackson 的 JavaType
        JavaType javaType = jsonMapper.getTypeFactory().constructType(type);
        // convertValue 会智能处理：
        // - 如果 variable 已经是目标类型（或可赋值），直接返回原对象
        // - 否则进行 JSON 风格的转换（例如 Map → POJO、List 元素类型转换等）
        return jsonMapper.convertValue(variable, javaType);
    }

    public static void setMapper(@NonNull JsonMapper mapper) {
        MAPPER.set(mapper);
    }
}
