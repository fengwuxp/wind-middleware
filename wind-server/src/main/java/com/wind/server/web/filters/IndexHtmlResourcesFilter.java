package com.wind.server.web.filters;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableSet;
import com.wind.common.exception.AssertUtils;
import com.wind.common.util.StringJoinSplitUtils;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 前后端分离模式下用于返回前端的 index.html 页面
 * 仅支持 browser 路由模式，参见： https://juejin.cn/post/6844903648804208654
 *
 * @author wuxp
 * @date 2023-10-21 20:18
 **/
@AllArgsConstructor
public class IndexHtmlResourcesFilter extends OncePerRequestFilter {

    public static final String INDEX_HTML_NAME = "/index.html";

    /**
     * 默认缓存 60 天
     */
    private static final int CACHE_TIMES = 24 * 3600 * 60;

    /**
     * 请求 index.html 页面的请求路径
     */
    private static final Set<String> INDEX_HTML_PATHS = ImmutableSet.of("/", INDEX_HTML_NAME, "/index.htm", "/web");

    /**
     * 静态资源文件
     *
     * @key 路径
     * @value 内容
     */
    private static final Map<String, String> STATIC_RESOURCES = new ConcurrentHashMap<>();

    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 忽略 swagger、webjars 下的静态资源
     */
    private static final Set<String> IGNORE_PATTERNS = ImmutableSet.of("/swagger-ui/**", "/swagger-resources/**", "/webjars/**");

    static {
        STATIC_RESOURCES.put(".js", "application/javascript");
        STATIC_RESOURCES.put(".css", "text/css");
        STATIC_RESOURCES.put(".svg", "image/svg+xml");
        STATIC_RESOURCES.put(".webp", "image/webp");
        STATIC_RESOURCES.put(".gif", "image/webp");
        STATIC_RESOURCES.put(".png", "image/png");
        STATIC_RESOURCES.put(".jpg", "image/jpg");
        STATIC_RESOURCES.put(".jpeg", "image/jpeg");
        STATIC_RESOURCES.put(".ico", "image/ico");
        STATIC_RESOURCES.put(".ttf", "font/ttf");
        STATIC_RESOURCES.put(".otf", "font/otf");
        STATIC_RESOURCES.put(".woff", "font/woff");
        STATIC_RESOURCES.put(".ttc", "font/ttc");
    }

    private static final Cache<String, HttpHeaders> HEADER_CACHES =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).maximumSize(200).build();

    /**
     * 前端路由前缀，仅支持 browser 模式下的路由
     */
    private final String routePrefix;

    /**
     * 资源加载器
     */
    private final Function<String, byte[]> resourceLoader;


    public IndexHtmlResourcesFilter(Function<String, byte[]> resourceLoader) {
        this("/web/", resourceLoader);
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        if (Objects.equals(request.getMethod(), HttpMethod.GET.name())) {
            String requestUri = request.getRequestURI();
            if (IGNORE_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, requestUri))) {
                // 忽略
                chain.doFilter(request, response);
                return;
            }
            boolean requestIndexHtml =
                    matchesMediaType(request.getHeader(HttpHeaders.ACCEPT)) && (INDEX_HTML_PATHS.contains(requestUri) || requestUri.startsWith(routePrefix));
            if (requestIndexHtml) {
                // 写回 index.html
                response.getOutputStream().write(resourceLoader.apply(INDEX_HTML_NAME));
                writeHeaders(response, INDEX_HTML_NAME);
                return;
            }
            Optional<String> optional = STATIC_RESOURCES.keySet().stream().filter(requestUri::endsWith).findFirst();
            if (optional.isPresent()) {
                // js css 资源访问
                response.getOutputStream().write(resourceLoader.apply(requestUri));
                writeHeaders(response, requestUri);
                response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + CACHE_TIMES);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeHeaders(@Nonnull HttpServletResponse response, String requestUri) {
        HttpHeaders headers = HEADER_CACHES.getIfPresent(requestUri);
        if (headers == null) {
            return;
        }
        headers.forEach((name, values) -> {
            if (!ObjectUtils.isEmpty(values)) {
                response.setHeader(name, CollectionUtils.firstElement(values));
            }
        });
    }

    private boolean matchesMediaType(String mediaType) {
        return StringJoinSplitUtils.split(mediaType).stream().map(MediaType::parseMediaType).anyMatch(media -> media.includes(MediaType.TEXT_HTML));
    }


    /**
     * 返回一个支持缓存的资源加载器
     *
     * @param delegate 委托的资源加载器
     * @return 资源加载器
     */
    public static Function<String, byte[]> cacheWrapper(Function<String, ResponseEntity<ByteArrayResource>> delegate) {
        return new CacheResourcesLoader(delegate);
    }

    @AllArgsConstructor
    private static class CacheResourcesLoader implements Function<String, byte[]> {

        private final Function<String, ResponseEntity<ByteArrayResource>> delegate;

        /**
         * 资源缓存
         */
        private final Cache<String, byte[]> resourcesCaches = Caffeine.newBuilder().expireAfterWrite(Duration.ofDays(1)).maximumSize(200).build();

        @Override
        public byte[] apply(String resourcePath) {
            return resourcesCaches.get(resourcePath, key -> {
                ResponseEntity<ByteArrayResource> resp = delegate.apply(resourcePath);
                HEADER_CACHES.put(key, resp.getHeaders());
                AssertUtils.isTrue(resp.hasBody(), () -> String.format("load resource： %s failure", key));
                ByteArrayResource result = resp.getBody();
                AssertUtils.notNull(result, "load html resource must not null");
                return result.getByteArray();
            });
        }
    }
}
