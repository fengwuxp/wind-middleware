package com.wind.core;


import org.jspecify.annotations.NonNull;
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
    @NonNull
    WritableContextVariables putVariable(@NonNull String name, @Nullable Object val);

    /**
     * 移除变量
     *
     * @param name 变量名
     * @return this
     */
    @NonNull
    WritableContextVariables removeVariable(@NonNull String name);

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
