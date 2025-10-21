package com.wind.mask.masker.json;


import com.wind.mask.ObjectMasker;

import java.util.Collection;
import java.util.Map;

/**
 * Map 对象脱敏
 * 注意：该操作会改变原有的 Map 对象，请谨慎使用
 *
 * @author wuxp
 * @date 2024-08-02 15:03
 * @see JsonStringMasker
 **/
public final class MapObjectMasker implements ObjectMasker<Map<String, Object>, Object> {

    @Override
    public Object mask(Map<String, Object> map, Collection<String> keys) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        return MaskJsonJsonUtils.mask(keys, map);
    }
}
