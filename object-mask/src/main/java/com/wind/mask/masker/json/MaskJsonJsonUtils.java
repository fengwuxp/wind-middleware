package com.wind.mask.masker.json;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.wind.mask.WindMasker;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;

/**
 * @author wuxp
 * @date 2025-10-21 14:08
 **/
@Slf4j
final class MaskJsonJsonUtils {

    private MaskJsonJsonUtils() {
        throw new AssertionError();
    }

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
    static final Configuration JSON_PATH_CONFIG = Configuration.builder()
            // 路径不存在时返回 null，不抛异常
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    /**
     * 获取 JsonPath
     *
     * @param jsonpath jsonpath
     * @return JsonPath
     */
    static JsonPath path(String jsonpath) {
        return JSON_PATHS.get(jsonpath, JsonPath::compile);
    }

    /**
     * 脱敏
     *
     * @param keys 待脱敏的 key
     * @param json json
     * @return 脱敏后的 json
     */
    static String mask(Collection<String> keys, String json) {
        DocumentContext doc = JsonPath.using(JSON_PATH_CONFIG).parse(json);
        mask(keys, doc);
        return doc.jsonString();
    }

    /**
     * 脱敏
     *
     * @param keys 待脱敏的 key
     * @param json json
     * @return 脱敏后的 json
     */
    static Object mask(Collection<String> keys, Object json) {
        DocumentContext doc = JsonPath.using(JSON_PATH_CONFIG).parse(json);
        mask(keys, doc);
        return doc.json();
    }

    private static void mask(Collection<String> keys, DocumentContext context) {
        for (String key : keys) {
            // 获取或缓存 JsonPath
            JsonPath path = MaskJsonJsonUtils.path(key);
            try {
                Object value = context.read(path);
                if (value != null) {
                    context.set(path, WindMasker.ASTERISK.mask(value));
                }
            } catch (Exception e) {
                log.warn("Mask failed for json path = {}, message = {}", key, e.getMessage(), e);
            }
        }
    }

}
