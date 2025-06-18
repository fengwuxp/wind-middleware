package com.wind.core;

import com.wind.common.exception.AssertUtils;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * readonly metadata
 *
 * @author wuxp
 * @date 2024-06-13 09:12
 **/
public interface ObjectReadonlyMetadata {

    /**
     * 获取元数据
     *
     * @param name 变量名称
     * @return 元数据
     */
    @Nullable
    default <T> T getMetadataValue(@NotBlank String name) {
        return getMetadataValue(name, null);
    }

    /**
     * 获取元数据
     *
     * @param name         变量名称
     * @param defaultValue 默认值
     * @return 元数据
     */
    @SuppressWarnings("unchecked")
    default <T> T getMetadataValue(@NotBlank String name, @Nullable T defaultValue) {
        AssertUtils.hasText(name, "argument context variable name must not empty");
        Map<String, Object> metadata = getMetadata();
        return metadata == null ? null : (T) metadata.getOrDefault(name, defaultValue);
    }

    /**
     * 获取元数据，如果 name 对应的变量不存在则抛异常
     *
     * @param name 变量名称
     * @return 元数据
     */
    default <T> T requireMetadataValue(@NotBlank String name) {
        T result = getMetadataValue(name);
        AssertUtils.notNull(result, () -> String.format("context variable %s must not null", name));
        return result;
    }

    /**
     * @return 获取所有的只读元数据
     */
    @Nullable
    Map<String, Object> getMetadata();
}
