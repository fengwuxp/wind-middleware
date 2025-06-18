package com.wind.core;

import com.wind.common.exception.AssertUtils;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

/**
 * readonly context variables
 *
 * @author wuxp
 * @date 2024-06-13 09:12
 **/
public interface ReadonlyContextVariables {

    /**
     * 获取上下文变量
     *
     * @param name 变量名称
     * @return 上下文变量
     */
    @Nullable
    default <T> T getContextVariable(@NotBlank String name) {
        return getContextVariable(name, null);
    }

    /**
     * 获取上下文变量
     *
     * @param name         变量名称
     * @param defaultValue 默认值
     * @return 上下文变量
     */
    @SuppressWarnings("unchecked")
    default <T> T getContextVariable(@NotBlank String name, @Nullable T defaultValue) {
        AssertUtils.hasText(name, "argument context variable name must not empty");
        Map<String, Object> contextVariables = getContextVariables();
        return contextVariables == null ? null : (T) contextVariables.getOrDefault(name, defaultValue);
    }

    /**
     * 获取上下文变量，如果 name 对应的变量不存在则抛异常
     *
     * @param name 变量名称
     * @return 上下文变量
     */
    default <T> T requireContextVariable(@NotBlank String name) {
        T result = getContextVariable(name);
        AssertUtils.notNull(result, () -> String.format("context variable %s must not null", name));
        return result;
    }

    /**
     * @return 获取所有的只读上下文变量
     */
    @Nullable
    Map<String, Object> getContextVariables();

    @NotNull
    static ReadonlyContextVariables empty() {
        return Collections::emptyMap;
    }

    @NotNull
    static ReadonlyContextVariables of(@NotNull Map<String, Object> contextVariables) {
        AssertUtils.notNull(contextVariables, "argument contextVariables must not null");
        return () -> contextVariables;
    }
}
