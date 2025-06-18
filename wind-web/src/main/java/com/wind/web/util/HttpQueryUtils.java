package com.wind.web.util;

import com.wind.common.WindConstants;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * http 查询参数工具
 *
 * @author wuxp
 * @date 2024-02-21 15:57
 **/
public final class HttpQueryUtils {

    private HttpQueryUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static MultiValueMap<String, String> parseQueryParamsFormUri(String uri) {
        if (StringUtils.hasText(uri)) {
            return UriComponentsBuilder.fromUriString(UriUtils.decode(uri, StandardCharsets.UTF_8))
                    .build()
                    .getQueryParams();
        }
        return new LinkedMultiValueMap<>();
    }

    @NotNull
    public static MultiValueMap<String, String> parseQueryParams(String queryString) {
        if (StringUtils.hasText(queryString)) {
            return parseQueryParamsFormUri(String.format("%s%s%s", WindConstants.SLASH, WindConstants.QUESTION_MARK, queryString));
        }
        return new LinkedMultiValueMap<>();
    }

    @NotNull
    public static Map<String, String[]> parseQueryParamsAsMap(String queryString) {
        Map<String, String[]> result = new HashMap<>();
        parseQueryParams(queryString)
                .forEach((key, values) -> {
                    if (!ObjectUtils.isEmpty(values)) {
                        result.put(key, values.toArray(new String[0]));
                    }
                });
        return result;
    }
}
