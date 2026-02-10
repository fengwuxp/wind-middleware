package com.wind.mask.masker.json;

import com.wind.mask.ObjectMasker;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * json text 脱敏
 *
 * @author wuxp
 * @date 2024-08-02 15:04
 **/
public final class JsonStringMasker implements ObjectMasker<String, String> {


    @Override
    public String mask(String json, Collection<String> keys) {
        if (!StringUtils.hasText(json) || CollectionUtils.isEmpty(keys)) {
            return json;
        }
        return MaskJsonJsonUtils.mask(keys, json);
    }
}
