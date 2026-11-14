package com.wind.web.filters;

import com.wind.common.exception.AssertUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 基于 Spring {@link PathPattern} 的路径委托过滤器。
 * 黑名单优先；白名单为空时表示不限制请求路径，未匹配的请求跳过被委托 Filter。
 *
 * <p>规则支持两种形式：{@code /api/**} 匹配任意 HTTP 方法，{@code GET /api/**} 同时匹配 HTTP 方法和路径。</p>
 *
 * <p>仅按路径委托：</p>
 * <pre>{@code
 * PathPatternDelegatingFilter.whitelist(delegate, List.of("/api/**"));
 * }</pre>
 *
 * <p>按 HTTP 方法和路径委托：</p>
 * <pre>{@code
 * PathPatternDelegatingFilter.whitelist(delegate, List.of("POST /api/**"));
 * PathPatternDelegatingFilter.blacklist(delegate, List.of("GET /admin/**"));
 * }</pre>
 * 查询参数不参与匹配；黑名单匹配时不会执行被委托 Filter，而是继续执行后续 FilterChain。
 *
 * @author wuxp
 * @since 2026-11-12
 */
public class PathPatternDelegatingFilter extends OncePerRequestFilter {

    // 包装器和委托共享 FilterConfig，但不能共享已执行标记；多个包装实例也须独立。
    private final String alreadyFilteredAttributeName = getClass().getName() + "." + UUID.randomUUID() + ALREADY_FILTERED_SUFFIX;

    private final Filter delegate;

    private final List<RequestPatternMatcher> whitelist;

    private final List<RequestPatternMatcher> blacklist;

    /**
     * @param delegate  被包装的 Filter
     * @param whitelist 白名单表达式，空集合表示不限制
     * @param blacklist 黑名单表达式，匹配后不执行被委托 Filter
     */
    public PathPatternDelegatingFilter(Filter delegate, Collection<String> whitelist, Collection<String> blacklist) {
        AssertUtils.notNull(delegate, "argument delegate must not null");
        this.delegate = delegate;
        this.whitelist = parsePatterns(whitelist);
        this.blacklist = parsePatterns(blacklist);
    }

    /**
     * 仅按白名单委托 Filter。
     *
     * @param delegate  被包装的 Filter
     * @param whitelist 白名单路径
     * @return 路径匹配包装器
     */
    public static PathPatternDelegatingFilter whitelist(Filter delegate, Collection<String> whitelist) {
        return new PathPatternDelegatingFilter(delegate, whitelist, Collections.emptyList());
    }

    /**
     * 仅按黑名单委托 Filter。
     *
     * @param delegate  被包装的 Filter
     * @param blacklist 黑名单路径
     * @return 路径匹配包装器
     */
    public static PathPatternDelegatingFilter blacklist(Filter delegate, Collection<String> blacklist) {
        return new PathPatternDelegatingFilter(delegate, Collections.emptyList(), blacklist);
    }

    @Override
    protected String getAlreadyFilteredAttributeName() {
        return alreadyFilteredAttributeName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws IOException, ServletException {
        if (matches(request)) {
            delegate.doFilter(request, response, chain);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        // Spring Bean 初始化时尚无 Servlet 配置，等容器调用 init 后再初始化委托。
        FilterConfig filterConfig = getFilterConfig();
        if (filterConfig != null) {
            delegate.init(filterConfig);
        }
    }

    @Override
    public void destroy() {
        try {
            delegate.destroy();
        } finally {
            super.destroy();
        }
    }

    private boolean matches(HttpServletRequest request) {
        if (blacklist.stream().anyMatch(pattern -> pattern.matches(request))) {
            return false;
        }
        return CollectionUtils.isEmpty(whitelist) || whitelist.stream().anyMatch(pattern -> pattern.matches(request));
    }

    private static List<RequestPatternMatcher> parsePatterns(Collection<String> patterns) {
        if (CollectionUtils.isEmpty(patterns)) {
            return Collections.emptyList();
        }
        return patterns.stream()
                .distinct()
                .map(PathPatternDelegatingFilter::parsePattern)
                .toList();
    }

    private static RequestPatternMatcher parsePattern(String expression) {
        AssertUtils.hasText(expression, "request pattern expression must not empty");
        String normalized = expression.trim();
        String[] parts = normalized.split("\\s+", 2);
        if (parts.length == 1) {
            return new RequestPatternMatcher(null, PathPatternParser.defaultInstance.parse(normalized));
        }
        HttpMethod method = HttpMethod.valueOf(parts[0].toUpperCase(Locale.ROOT));
        String path = parts[1];
        AssertUtils.hasText(path, "request pattern path must not empty");
        return new RequestPatternMatcher(method, PathPatternParser.defaultInstance.parse(path));
    }

    private record RequestPatternMatcher(@Nullable HttpMethod method, PathPattern pathPattern) {

        private boolean matches(HttpServletRequest request) {
            return (method == null || method.matches(request.getMethod())) && pathPattern.matches(PathContainer.parsePath(request.getRequestURI()));
        }
    }
}
