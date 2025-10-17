package com.wind.server.web.filters;

import com.wind.middleware.idempotent.WindIdempotentExecuteUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_EXECUTE_PREFIX;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IDEMPOTENT_RESULT_ATTRIBUTE_NAME;

/**
 * http 请求幂等处理, 请求头如果存在 {@link com.wind.common.WindHttpConstants#HTTP_REQUEST_IDEMPOTENT_HEADER_NAME} 则以幂等的方式执行
 *
 * @author wuxp
 * @date 2025-10-17 09:03
 **/
@Slf4j
public class HttpRequestIdempotentFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain chain) throws ServletException,
            IOException {
        String idempotentKey = request.getHeader(HTTP_REQUEST_IDEMPOTENT_HEADER_NAME);
        if (StringUtils.hasText(idempotentKey)) {
            // 如果幂等 key 存在，则执行幂等处理
            AtomicBoolean isIdempotent = new AtomicBoolean(true);
            Object result = WindIdempotentExecuteUtils.executeWithThrows(HTTP_REQUEST_IDEMPOTENT_EXECUTE_PREFIX + idempotentKey, () -> {
                isIdempotent.set(false);
                chain.doFilter(request, response);
                return request.getAttribute(HTTP_REQUEST_IDEMPOTENT_RESULT_ATTRIBUTE_NAME);
            });
            if (!response.isCommitted() && isIdempotent.get()) {
                // 幂等处理成功，则返回结果
                if (result == null) {
                    HttpResponseMessageUtils.writeApiResp(response, RestfulApiRespFactory.ok());
                } else {
                    HttpResponseMessageUtils.writeJson(response, result);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
