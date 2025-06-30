package com.wind.server.web.restful;


import com.wind.server.web.supports.ApiResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.TRUE;
import static com.wind.common.WindConstants.WIND_SERVER_HTTP_RESPONSE_STATUS_ADVICE;


/**
 * 统一处理 restful api 风格的响应
 * 如果返回的是{@link ApiResp}类型的数据，将同步设置 http status
 *
 * @author wuxp
 */
@Slf4j
@ConditionalOnProperty(prefix = WIND_SERVER_HTTP_RESPONSE_STATUS_ADVICE, value = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
@RestControllerAdvice()
public class HttpResponseStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, @Nonnull Class converterType) {
        return Objects.requireNonNull(returnType.getMethod()).getReturnType().isAssignableFrom(ApiResp.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, @Nonnull MethodParameter returnType, @Nonnull MediaType selectedContentType,
                                  @Nonnull Class selectedConverterType, @Nonnull ServerHttpRequest request,
                                  @Nonnull ServerHttpResponse response) {
        if (body instanceof ApiResp<?> resp) {
            response.setStatusCode(resp.getHttpStatus());
        }
        return body;
    }
}
