package com.wind.common.config;

import com.wind.common.exception.AssertUtils;
import com.wind.common.util.StringJoinSplitUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 应用系统配置 Repository
 *
 * @author wuxp
 * @date 2023-11-15 09:36
 **/
public interface SystemConfigRepository extends SystemConfigStorage {


    /**
     * 获取配置，如果配置为 null 抛出异常
     *
     * @param name 配置名称
     * @return 配置值
     */
    @NotNull
    default String requireConfig(@NotBlank String name) {
        String result = getConfig(name);
        AssertUtils.notNull(result, () -> String.format("config %s not found", name));
        return result;
    }

    /**
     * 获取配置并转换
     *
     * @param name  配置名称
     * @param clazz 类类型
     * @return 配置值
     */
    @Nullable
    default <T> T getConfig(@NotBlank String name, @NotNull Class<T> clazz) {
        return getConfig(name, clazz, null);
    }

    /**
     * 获取配置并转换
     *
     * @param name       配置名称
     * @param targetType 类类型
     * @return 配置值
     */
    @Nullable
    <T> T getConfig(@NotBlank String name, @NotNull Class<T> targetType, T defaultValue);

    /**
     * 获取集合类型配置
     *
     * @param name       配置名称
     * @param targetType 类类型
     * @return 配置值
     */
    @NotEmpty
    <T> Set<T> getConfigs(@NotBlank String name, @NotNull Class<T> targetType);

    /**
     * 获取集合类型配置
     *
     * @param name 配置名称
     * @return 配置值
     */
    @NotEmpty
    default Set<String> getConfigs(@NotBlank String name) {
        String config = getConfig(name);
        return StringJoinSplitUtils.split(config);
    }

    @Nullable
    default Boolean asBoolean(@NotBlank String name) {
        return getConfig(name, Boolean.TYPE);
    }

    @Nullable
    default Integer asInt(@NotBlank String name) {
        return getConfig(name, Integer.TYPE);
    }

    @Nullable
    default Long asLong(@NotBlank String name) {
        return getConfig(name, Long.TYPE);
    }

    /**
     * 获取 json 配置并转换
     *
     * @param name       配置名称
     * @param targetType 类类型
     * @return 配置值
     */
    @Nullable
    <T> T getJsonConfig(@NotBlank String name, @NotNull Class<T> targetType);

    /**
     * 获取 json 配置并转换
     *
     * @param name       配置名称
     * @param targetType 类类型
     * @return 配置值
     */
    @Nullable
    <T> T getJsonConfig(@NotBlank String name, @NotNull ParameterizedTypeReference<T> targetType);

    /**
     * 获取 json 配置并转换为 map 类型
     *
     * @param name 配置名称
     * @return 配置值
     */
    @NotNull
    default Map<String, Object> getJsonConfig(@NotBlank String name) {
        Map<String, Object> result = getJsonConfig(name, new ParameterizedTypeReference<Map<String, Object>>() {
        });
        return result == null ? Collections.emptyMap() : result;
    }
}
