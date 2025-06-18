package com.wind.security.authentication;


import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 统一认证失败、认证失效处理器
 *
 * @author wuxp
 * @date 2023-09-28 09:29
 **/
@Slf4j
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler, AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        log.warn("authentication error", exception);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        HttpResponseMessageUtils.writeJson(response, RestfulApiRespFactory.unAuthorized(SpringI18nMessageUtils.getMessage("未登录或登录已失效")));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        log.warn("access denied exception", exception);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        HttpResponseMessageUtils.writeJson(response, RestfulApiRespFactory.unAuthorized(SpringI18nMessageUtils.getMessage("您没有访问该资源的权限")));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException,
            ServletException {
        log.warn("commence authentication error", exception);
        onAuthenticationFailure(request, response, exception);
    }

}
