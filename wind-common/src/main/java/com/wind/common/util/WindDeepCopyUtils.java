package com.wind.common.util;

import com.wind.common.codec.KryoCodec;
import org.springframework.lang.Nullable;

/**
 * 深 copy 工具类
 *
 * @author wuxp
 * @date 2024-08-07 14:42
 **/
public final class WindDeepCopyUtils {

    private WindDeepCopyUtils() {
        throw new AssertionError();
    }

    /**
     * java 深 copy 工具类
     * 注意：被 copy 对象的类类型必须存在可见的构造
     *
     * @param object 原对象
     * @return 深 copy 后的新对象
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T copy(@Nullable T object) {
        if (object == null) {
            return null;
        }
        KryoCodec codec = KryoCodec.getInstance();
        return (T) codec.decode(codec.encode(object));
    }
}
