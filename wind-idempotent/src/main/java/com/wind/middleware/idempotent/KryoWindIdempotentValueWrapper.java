package com.wind.middleware.idempotent;


import com.wind.common.codec.KryoCodec;
import com.wind.common.exception.AssertUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.beans.Transient;
import java.util.Base64;

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
        byte[] bytes = KryoCodec.getInstance().encode(value);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @NotNull
    public static KryoWindIdempotentValueWrapper of(@NotBlank String text) {
        AssertUtils.hasText(text, "argument text must not empty");
        byte[] bytes = Base64.getDecoder().decode(text);
        return of(bytes);
    }

    @NotNull
    public static KryoWindIdempotentValueWrapper of(@NotNull byte[] bytes) {
        AssertUtils.isTrue(bytes != null && bytes.length > 0, "argument bytes must not empty");
        return new KryoWindIdempotentValueWrapper(KryoCodec.getInstance().decode(bytes));
    }

}
