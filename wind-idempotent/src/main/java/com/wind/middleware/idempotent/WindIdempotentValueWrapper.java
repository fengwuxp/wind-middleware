package com.wind.middleware.idempotent;


import jakarta.annotation.Nullable;

/**
 * 幂等值包装类
 *
 * @author wuxp
 * @date 2025-10-13 10:56
 **/
public interface WindIdempotentValueWrapper {

    /**
     * @return 幂等执行结果值
     */
    @Nullable
    <T> T getValue();

    /**
     * 空幂等值包装类
     *
     * @return 空幂等值包装类
     */
    static WindIdempotentValueWrapper empty() {
        return new WindIdempotentValueWrapper() {
            @Override
            public <T> T getValue() {
                return null;
            }
        };
    }
}
