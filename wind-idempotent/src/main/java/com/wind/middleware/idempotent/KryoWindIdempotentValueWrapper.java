package com.wind.middleware.idempotent;


import com.wind.common.util.KryoSerializationUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.beans.Transient;

/**
 * 使用 Kryo 序列化的幂等值包装类
 * 可以将任意对象序列化为字节数组存储，再反序列化恢复
 * 支持不可变集合、内部类等
 *
 * @param value 缓存反序列化后的对象
 * @author wuxp
 * @date 2025-10-13
 */
public record KryoWindIdempotentValueWrapper(@NotNull Object value) implements WindIdempotentValueWrapper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    /**
     * 获取对象序列化后的字符串
     */
    @Transient
    public String asText() {
        return KryoSerializationUtils.getInstance().encodeToString(value);
    }

    @NotNull
    public static KryoWindIdempotentValueWrapper of(@NotBlank String text) {
        return new KryoWindIdempotentValueWrapper(KryoSerializationUtils.getInstance().decode(text));
    }

    @NotNull
    public static KryoWindIdempotentValueWrapper of(@NotNull byte[] bytes) {
        return new KryoWindIdempotentValueWrapper(KryoSerializationUtils.getInstance().decode(bytes));
    }

}
