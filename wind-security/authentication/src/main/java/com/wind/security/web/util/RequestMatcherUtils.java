package com.wind.security.web.util;

import com.wind.common.WindConstants;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
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

    public static Set<RequestMatcher> convertMatchers(Set<String> patterns) {
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
