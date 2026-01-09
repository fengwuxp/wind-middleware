package com.wind.core;

import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 默认可写上下文变量实现
 *
 * @author wuxp
 * @date 2025-09-02 15:50
 **/
record DefaultWritableContextVariables(Map<String, Object> contextVariables) implements WritableContextVariables {
    @Override
    @NonNull
    public WritableContextVariables putVariable(@NonNull String name, Object val) {
        contextVariables.put(name, val);
        return this;
    }

    @Override
    @NonNull
    public WritableContextVariables removeVariable(@NonNull String name) {
        contextVariables.remove(name);
        return this;
    }

    @Override
    @NonNull
    public Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}