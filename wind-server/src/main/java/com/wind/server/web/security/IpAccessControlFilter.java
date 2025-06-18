package com.wind.server.web.security;

import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import com.wind.web.util.HttpServletRequestUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;

/**
 * 请求来源 ip 访问控制
 *
 * @author wuxp
 * @date 2024-03-15 17:55
 **/
@AllArgsConstructor
@Slf4j
public class IpAccessControlFilter extends OncePerRequestFilter {

    private final Function<HttpServletRequest, IpAccessControlConfig> ipConfigSupplier;

    public IpAccessControlFilter(IpAccessControlConfig config) {
        this(request -> config);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        if (isAllow(request)) {
            chain.doFilter(request, response);
        } else {
            HttpResponseMessageUtils.writeApiResp(response, RestfulApiRespFactory.badRequest("client source ip not allow access"));
        }
    }

    private boolean isAllow(HttpServletRequest request) {
        String clientIp = HttpServletRequestUtils.getRequestAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, request);
        if (clientIp == null || clientIp.isEmpty()) {
            return true;
        }
        IpAccessControlConfig config = this.ipConfigSupplier.apply(request);
        if (config == null || (CollectionUtils.isEmpty(config.getWhitelist()) && CollectionUtils.isEmpty(config.getBlacklist()))) {
            // 配置为 null 或 空
            return true;
        }
        if (matches(config.getBlacklist(), clientIp)) {
            // 黑名单优先级高，匹配则拒绝访问
            return false;
        }
        return matches(config.getWhitelist(), clientIp);
    }

    private boolean matches(Collection<IpAddressMatcher> requestMatchers, String clientIp) {
        return requestMatchers.stream().anyMatch(matcher -> matcher.matches(clientIp));
    }

    @Getter
    public static class IpAccessControlConfig {

        private final Collection<IpAddressMatcher> whitelist;

        private final Collection<IpAddressMatcher> blacklist;

        public IpAccessControlConfig(Collection<String> whitelist, Collection<String> blacklist) {
            this.whitelist = nullSafeGet(whitelist).stream().map(IpAddressMatcher::new).collect(Collectors.toList());
            this.blacklist = nullSafeGet(blacklist).stream().map(IpAddressMatcher::new).collect(Collectors.toList());
        }

        @NotNull
        private static Collection<String> nullSafeGet(Collection<String> ips) {
            return ips == null ? Collections.emptyList() : ips;
        }
    }
}
