package com.wind.security.web.util;

import com.wind.common.WindConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 请求匹配相关工具
 *
 * @author wuxp
 * @date 2023-10-02 08:59
 **/
public final class RequestMatcherUtils {

    private RequestMatcherUtils() {
        throw new AssertionError();
    }

    @Deprecated
    public static Set<RequestMatcher> convertMatchers(@NonNull Set<String> patterns) {
        return convertAntMatchers(patterns);
    }

    /**
     * 提供基于 Ant 风格或 Spring PathPattern 的请求路径匹配器生成方法
     *
     * @param patterns 支持包含 HTTP 方法 + 路径的字符串格式（如 "GET /api/**"）；
     * @return 匹配器列表
     */
    @Deprecated
    public static Set<RequestMatcher> convertAntMatchers(@Nullable Set<String> patterns) {
        if (patterns == null) {
            return Set.of();
        }
        return patterns
                .stream()
                .map(pattern -> {
                    if (pattern.contains(WindConstants.SPACE)) {
                        String[] parts = pattern.split(WindConstants.SPACE);
                        return new AntPathRequestMatcher(parts[1], parts[0]);
                    }
                    return new AntPathRequestMatcher(pattern);
                })
                .collect(Collectors.toSet());
    }


    /**
     * 提供基于 Spring PathPattern 的请求路径匹配器生成方法
     *
     * @param patterns 支持包含 HTTP 方法 + 路径的字符串格式（如 "GET /api/**"）；
     * @return 匹配器列表
     */
    public static Set<RequestMatcher> convertPathMatchers(@Nullable Set<String> patterns) {
        if (patterns == null) {
            return Set.of();
        }
        return patterns.stream()
                .map(pattern -> {
                    if (pattern.contains(WindConstants.SPACE)) {
                        String[] parts = pattern.split(WindConstants.SPACE);
                        return PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.valueOf(parts[0]), parts[1]);
                    }
                    return PathPatternRequestMatcher.withDefaults().matcher(pattern);
                })
                .collect(Collectors.toSet());
    }

    /**
     * 匹配请求
     *
     * @param matchers 匹配器列表
     * @param request  请求
     * @return 任意一个匹配器匹配则返回 true
     */
    public static boolean matches(Set<RequestMatcher> matchers, HttpServletRequest request) {
        return matchers.stream().anyMatch(requestMatcher -> requestMatcher.matches(request));
    }
}
