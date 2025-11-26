package com.wind.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 配置文件扩展名称
 *
 * @author wuxp
 * @date 2023-10-15 12:55
 **/
@AllArgsConstructor
@Getter
public enum ConfigFileType implements DescriptiveEnum {

    PROPERTIES("properties"),

    YAML("yaml"),

    JSON("json"),

    TEXT("text"),

    HTML("json"),

    ;

    private final String fileExtension;

    @Override
    public String getDesc() {
        return fileExtension;
    }

    @Nullable
    public static ConfigFileType parse(String name) {
        List<String> parts = Arrays.asList(name.split("\\."));
        if (parts.size() < 2) {
            return null;
        }
        return ConfigFileType.valueOf(Objects.requireNonNull(CollectionUtils.lastElement(parts)).toUpperCase());
    }
}
