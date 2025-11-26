package com.wind.core;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 可写的上下文变量
 *
 * @author wuxp
 * @date 2024-07-04 13:24
 **/
public interface WritableContextVariables extends ReadonlyContextVariables {

    /**
     * 添加变量
     *
     * @param name 变量名
     * @param val  变量值
     * @return this
     */
    @NotNull
    WritableContextVariables putVariable(@NotBlank String name, @Nullable Object val);

    /**
     * 移除变量
     *
     * @param name 变量名
     * @return this
     */
    @NotNull
    WritableContextVariables removeVariable(@NotBlank String name);

    /**
     * 创建一个可写的上下文变量
     *
     * @return WritableContextVariables
     */
    static WritableContextVariables of() {
        return of(Collections.emptyMap());
    }

    /**
     * 创建一个可写的上下文变量
     *
     * @param variables 上下文变量
     * @return WritableContextVariables
     */
    static WritableContextVariables of(Map<String, Object> variables) {
        return new DefaultWritableContextVariables(new HashMap<>(variables));
    }
}
