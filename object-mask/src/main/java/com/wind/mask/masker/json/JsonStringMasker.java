package com.wind.mask.masker.json;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.wind.mask.ObjectMasker;
import com.wind.mask.WindMasker;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;

/**
 * json text 脱敏
 *
 * @author wuxp
 * @date 2024-08-02 15:04
 **/
public final class JsonStringMasker implements ObjectMasker<String, String> {

    /**
     * JSONPath 编译缓存 (减少重复编译)
     */
    private static final Cache<@NotNull String, JsonPath> JSON_PATHS = Caffeine.newBuilder()
            .maximumSize(1000)
            .initialCapacity(100)
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    /**
     * Jayway JsonPath 配置，禁止异常抛出，可读取不存在的路径
     */
    private static final Configuration JSON_PATH_CONFIG = Configuration.builder()
            // 路径不存在时返回 null，不抛异常
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    @Override
    public String mask(String json, Collection<String> keys) {
        if (!StringUtils.hasText(json) || CollectionUtils.isEmpty(keys)) {
            return json;
        }

        DocumentContext doc;
        try {
            doc = JsonPath.using(JSON_PATH_CONFIG).parse(json);
        } catch (Exception e) {
            // 非法 JSON，直接返回原文
            return json;
        }

        for (String key : keys) {
            // 获取或缓存 JsonPath
            JsonPath path = JSON_PATHS.get(key.trim(), JsonPath::compile);
            try {
                Object value = doc.read(path);
                if (value != null) {
                    doc.set(path, WindMasker.ASTERISK.mask(value));
                }
            } catch (Exception e) {
                // 路径不存在或类型不匹配等异常，安全忽略
                // log.debug("Mask failed for path [{}]: {}", key, e.getMessage());
            }
        }

        return doc.jsonString();
    }
}
