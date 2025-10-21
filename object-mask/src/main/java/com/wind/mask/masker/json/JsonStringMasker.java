package com.wind.mask.masker.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONPath;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.mask.ObjectMasker;
import com.wind.mask.WindMasker;
import jakarta.validation.constraints.NotNull;
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
     * json path缓存(减少动态访问器类)
     */
    private static final Cache<@NotNull String, JSONPath> JSON_PATHS = Caffeine.newBuilder()
            .maximumSize(1000)
            .initialCapacity(100)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    @Override
    public String mask(String json, Collection<String> keys) {
        if (StringUtils.hasText(json)) {
            Object val = JSON.parse(json);
            keys.forEach(key -> {
                JSONPath path = JSON_PATHS.get(key, JSONPath::of);
                try {
                    Object eval = path.eval(val);
                    if (eval != null) {
                        path.set(val, WindMasker.ASTERISK.mask(eval));
                    }
                } catch (Exception exception) {
                    // ignore
                }
            });
            return JSON.toJSONString(val);
        }
        return json;
    }
}