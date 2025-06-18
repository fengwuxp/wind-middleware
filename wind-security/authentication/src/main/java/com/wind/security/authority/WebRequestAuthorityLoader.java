package com.wind.security.authority;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 用于加载 web 请求需要的权限
 *
 * @author wuxp
 * @date 2024-12-15 13:34
 **/
public interface WebRequestAuthorityLoader {

    /**
     * 加载请求需要的权限
     *
     * @param request http 请求对象
     * @return 请求需要的权限集合，不需要权限则返回空
     */
    Collection<String> load(HttpServletRequest request);

    default Collection<GrantedAuthority> loadAuthorities(HttpServletRequest request) {
        return load(request).stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }
}
