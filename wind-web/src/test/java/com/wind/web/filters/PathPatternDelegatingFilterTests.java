package com.wind.web.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证路径委托过滤器对白名单、黑名单和 Spring PathPattern 的匹配行为。
 *
 * @author wuxp
 * @since 2026-11-12
 */
class PathPatternDelegatingFilterTests {

    @Test
    void testWhitelistMatchExecutesDelegate() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = PathPatternDelegatingFilter.whitelist(
                new CountingFilter(new AtomicInteger(), new AtomicInteger(), delegateExecutions), List.of("/api/**"));

        wrapper.doFilter(request("/api/users"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(1, delegateExecutions.get());
        assertEquals(0, chainExecutions.get());
    }

    @Test
    void testWhitelistMissSkipsDelegateAndContinuesChain() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = PathPatternDelegatingFilter.whitelist(
                new CountingFilter(new AtomicInteger(), new AtomicInteger(), delegateExecutions), List.of("/api/**"));

        wrapper.doFilter(request("/web/index.html"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(0, delegateExecutions.get());
        assertEquals(1, chainExecutions.get());
    }

    @Test
    void testBlacklistMatchTakesPriorityOverWhitelist() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = wrapper(delegateExecutions, List.of("/**"), List.of("/admin/**"));

        wrapper.doFilter(request("/admin/users"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(0, delegateExecutions.get());
        assertEquals(1, chainExecutions.get());
    }

    @Test
    void testBlacklistFactorySkipsMatchedPath() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = PathPatternDelegatingFilter.blacklist(
                new CountingFilter(new AtomicInteger(), new AtomicInteger(), delegateExecutions), List.of("/admin/**"));

        wrapper.doFilter(request("/admin/users"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(0, delegateExecutions.get());
        assertEquals(1, chainExecutions.get());
    }

    @Test
    void testMethodAndPathWhitelistOnlyMatchesConfiguredMethod() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = PathPatternDelegatingFilter.whitelist(
                new CountingFilter(new AtomicInteger(), new AtomicInteger(), delegateExecutions), List.of("POST /api/**"));

        wrapper.doFilter(request("POST", "/api/users"), new MockHttpServletResponse(), chain(chainExecutions));
        wrapper.doFilter(request("GET", "/api/users"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(1, delegateExecutions.get());
        assertEquals(1, chainExecutions.get());
    }

    @Test
    void testEmptyWhitelistAndBlacklistExecutesDelegate() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = wrapper(delegateExecutions, List.of(), List.of());

        wrapper.doFilter(request("/any/path"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(1, delegateExecutions.get());
        assertEquals(0, chainExecutions.get());
    }

    @Test
    void testPathPatternSupportsVariablesAndTrailingSegments() throws Exception {
        AtomicInteger delegateExecutions = new AtomicInteger();
        AtomicInteger chainExecutions = new AtomicInteger();
        PathPatternDelegatingFilter wrapper = wrapper(delegateExecutions, List.of("/users/{userId}"), List.of());

        wrapper.doFilter(request("/users/42"), new MockHttpServletResponse(), chain(chainExecutions));
        wrapper.doFilter(request("/users/42/orders"), new MockHttpServletResponse(), chain(chainExecutions));

        assertEquals(1, delegateExecutions.get());
        assertEquals(1, chainExecutions.get());
    }

    @Test
    void testFilterLifecycleDelegates() throws Exception {
        AtomicInteger initCalls = new AtomicInteger();
        AtomicInteger destroyCalls = new AtomicInteger();
        Filter delegate = new CountingFilter(initCalls, destroyCalls, new AtomicInteger());
        PathPatternDelegatingFilter wrapper = new PathPatternDelegatingFilter(delegate, List.of(), List.of());

        wrapper.init(new MockFilterConfig());
        wrapper.destroy();

        assertEquals(1, initCalls.get());
        assertEquals(1, destroyCalls.get());
    }

    /**
     * Spring 先初始化包装器 Bean，再由 Servlet 容器提供 FilterConfig。
     */
    @Test
    void testBeanInitializationWaitsForServletFilterConfig() throws Exception {
        OncePerRequestFilter delegate = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) {
                response.setStatus(202);
            }
        };
        PathPatternDelegatingFilter filter = PathPatternDelegatingFilter.whitelist(delegate, List.of("/api/**"));
        filter.afterPropertiesSet();
        MockFilterConfig config = new MockFilterConfig();
        filter.init(config);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/users"), response, (req, resp) -> resp.setContentType("text/plain"));

        assertEquals(config, delegate.getFilterConfig());
        assertEquals(202, response.getStatus());
    }

    @Test
    void testHttpMethodMatchingIsIndependentOfDefaultLocale() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            AtomicInteger delegateExecutions = new AtomicInteger();
            AtomicInteger chainExecutions = new AtomicInteger();
            PathPatternDelegatingFilter filter = wrapper(delegateExecutions, List.of("options /api/**"), List.of());

            filter.doFilter(request("OPTIONS", "/api/users"), new MockHttpServletResponse(), chain(chainExecutions));

            assertEquals(1, delegateExecutions.get());
            assertEquals(0, chainExecutions.get());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void testIndependentWrappersBothDelegateInSameRequest() throws Exception {
        PathPatternDelegatingFilter first = PathPatternDelegatingFilter.whitelist((request, response, chain) -> {
            ((HttpServletResponse) response).addHeader("First-Filter", "executed");
            chain.doFilter(request, response);
        }, List.of("/api/**"));
        PathPatternDelegatingFilter second = PathPatternDelegatingFilter.whitelist((request, response, chain) -> {
            ((HttpServletResponse) response).addHeader("Second-Filter", "executed");
            chain.doFilter(request, response);
        }, List.of("/api/**"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        first.doFilter(request("/api/users"), response, (req, resp) ->
                second.doFilter(req, resp, (r, s) -> ((HttpServletResponse) s).setStatus(204)));

        assertEquals("executed", response.getHeader("First-Filter"));
        assertEquals("executed", response.getHeader("Second-Filter"));
        assertEquals(204, response.getStatus());
    }

    private static PathPatternDelegatingFilter wrapper(AtomicInteger delegateExecutions,
                                                       List<String> whitelist,
                                                       List<String> blacklist) {
        return new PathPatternDelegatingFilter(
                new CountingFilter(new AtomicInteger(), new AtomicInteger(), delegateExecutions), whitelist, blacklist);
    }

    private static MockHttpServletRequest request(String requestUri) {
        return new MockHttpServletRequest("GET", requestUri);
    }

    private static MockHttpServletRequest request(String method, String requestUri) {
        return new MockHttpServletRequest(method, requestUri);
    }

    private static FilterChain chain(AtomicInteger executions) {
        return (request, response) -> executions.incrementAndGet();
    }

    private record CountingFilter(AtomicInteger initCalls, AtomicInteger destroyCalls, AtomicInteger executions) implements Filter {

        @Override
        public void init(FilterConfig filterConfig) {
            initCalls.incrementAndGet();
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
            executions.incrementAndGet();
        }

        @Override
        public void destroy() {
            destroyCalls.incrementAndGet();
        }
    }
}
