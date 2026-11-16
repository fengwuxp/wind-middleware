package com.wind.server.web.security;

import com.wind.common.util.IpAddressUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import com.wind.web.util.HttpServletRequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;

/**
 * 请求来源 IP 访问控制过滤器。
 *
 * <p>过滤器从 {@value com.wind.common.WindHttpConstants#HTTP_REQUEST_IP_ATTRIBUTE_NAME} 请求属性读取来源 IP；该属性通常由前置的请求追踪过滤器写入。</p>
 * <p>非严格模式下，来源 IP 缺失或为空时直接放行；严格模式下，来源 IP 必须是合法的 IPv4 或 IPv6 地址。</p>
 * <p>完成来源 IP 校验后，黑名单优先于白名单：黑名单命中时拒绝请求；白名单非空时仅允许命中的请求；两者均为空时放行。</p>
 * <p>白名单和黑名单支持单个 IP、CIDR 网段等 {@link IpAddressMatcher} 支持的表达式。</p>
 *
 * @author wuxp
 * @date 2024-03-15 17:55
 **/
@AllArgsConstructor
@Slf4j
public class IpAccessControlFilter extends OncePerRequestFilter {

    private final Function<HttpServletRequest, IpAccessControlConfig> ipConfigSupplier;

    private final boolean strict;

    /**
     * 创建兼容历史行为的非严格 IP 访问控制过滤器。
     *
     * @param config 白名单和黑名单配置，可以为 null
     */
    public IpAccessControlFilter(IpAccessControlConfig config) {
        this(config, false);
    }

    /**
     * 按请求动态获取 IP 访问控制配置，默认使用非严格模式。
     *
     * @param ipConfigSupplier 配置提供者，返回 null 表示不限制 IP 名单
     */
    public IpAccessControlFilter(Function<HttpServletRequest, IpAccessControlConfig> ipConfigSupplier) {
        this(ipConfigSupplier, false);
    }

    /**
     * 创建 IP 访问控制过滤器。
     *
     * @param config 白名单和黑名单配置，可以为 null
     * @param strict 是否启用严格模式；严格模式拒绝缺失或非法来源 IP
     */
    public IpAccessControlFilter(IpAccessControlConfig config, boolean strict) {
        this(request -> config, strict);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        if (isAllow(request)) {
            chain.doFilter(request, response);
        } else {
            log.error("client source ip not allow access, request ip = {}, request host = {}",
                    HttpServletRequestUtils.getRequestAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, request), HttpServletRequestUtils.getHeader(HttpHeaders.HOST));
            HttpResponseMessageUtils.writeApiResp(response, RestfulApiRespFactory.badRequest("client source ip not allow access"));
        }
    }

    private boolean isAllow(HttpServletRequest request) {
        String clientIp = HttpServletRequestUtils.getRequestAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, request);
        if (strict) {
            // 严格模式下必须要有合法的来源 ip
            if (!IpAddressUtils.isValidIp(clientIp)) {
                return false;
            }
        } else {
            // 非严格模式允许为空的 来源 ip
            if (!StringUtils.hasText(clientIp)) {
                return true;
            }
        }
        IpAccessControlConfig config = this.ipConfigSupplier.apply(request);
        if (config == null || (CollectionUtils.isEmpty(config.whitelist()) && CollectionUtils.isEmpty(config.blacklist()))) {
            // 配置为 null 或 空
            return true;
        }
        if (matches(config.blacklist(), clientIp)) {
            // 黑名单优先级高，匹配则拒绝访问
            return false;
        }
        return matches(config.whitelist(), clientIp);
    }

    private boolean matches(Collection<IpAddressMatcher> requestMatchers, String clientIp) {
        return requestMatchers.stream().anyMatch(matcher -> matcher.matches(clientIp));
    }

    /**
     * IP 访问控制规则配置。
     *
     * <p>record 组件保存原始规则表达式；{@link #whitelist()} 和 {@link #blacklist()} 保留历史 getter，
     * 返回可直接用于匹配的 {@link IpAddressMatcher} 集合。</p>
     *
     * @param whitelist 白名单 ip 地址匹配器列表
     * @param blacklist 黑名单 ip 地址匹配器列表
     */
    public record IpAccessControlConfig(@NonNull Collection<IpAddressMatcher> whitelist, @NonNull Collection<IpAddressMatcher> blacklist) {

        /**
         * @param whitelist 白名单 IP 或 CIDR 表达式
         * @param blacklist 黑名单 IP 或 CIDR 表达式
         * @return IpAccessControlConfig
         */
        public static IpAccessControlConfig of(Collection<String> whitelist, Collection<String> blacklist) {
            return new IpAccessControlConfig(toMatchers(nullSafeGet(whitelist)), toMatchers(nullSafeGet(blacklist)));
        }

        private static Collection<String> nullSafeGet(Collection<String> ips) {
            return ips == null ? Collections.emptyList() : ips;
        }

        private static Collection<IpAddressMatcher> toMatchers(Collection<String> patterns) {
            return patterns.stream().distinct().map(IpAddressMatcher::new).toList();
        }
    }
}
