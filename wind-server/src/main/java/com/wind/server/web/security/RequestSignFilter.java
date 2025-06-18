package com.wind.server.web.security;


import com.wind.api.core.signature.ApiSecretAccount;
import com.wind.api.core.signature.ApiSignatureRequest;
import com.wind.api.core.signature.SignatureHttpHeaderNames;
import com.wind.common.WindHttpConstants;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.common.util.ServiceInfoUtils;
import com.wind.server.servlet.RepeatableReadRequestWrapper;
import com.wind.server.web.filters.WindWebFilterOrdered;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpResponseMessageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;


/**
 * 接口请求验签
 * 参见：https://www.yuque.com/suiyuerufeng-akjad/wind/zl1ygpq3pitl00qp
 *
 * @author wuxp
 */
@Slf4j
@AllArgsConstructor
public class RequestSignFilter implements Filter, Ordered {

    /**
     * 签名时间戳 5 分钟内有效
     */
    public static final AtomicLong SIGNATURE_TIMESTAMP_VALIDITY_PERIOD = new AtomicLong(5 * 60 * 1000L);

    private static final String SIGAN_VERIFY_ERROR_MESSAGE = "sign verify error";

    private final SignatureHttpHeaderNames headerNames;

    private final ApiSecretAccountProvider apiSecretAccountProvider;

    /**
     * 忽略接口验签的请求匹配器
     */
    private final Collection<RequestMatcher> ignoreRequestMatchers;

    /**
     * 是否启用
     */
    private final boolean enable;

    public RequestSignFilter(ApiSecretAccountProvider accountProvider, Collection<RequestMatcher> ignoreRequestMatchers, boolean enable) {
        this(new SignatureHttpHeaderNames(), accountProvider, ignoreRequestMatchers, enable);
    }

    public RequestSignFilter(String headerPrefix, ApiSecretAccountProvider accountProvider, Collection<RequestMatcher> ignoreRequestMatchers,
                             boolean enable) {
        this(new SignatureHttpHeaderNames(headerPrefix), accountProvider, ignoreRequestMatchers, enable);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (!enable || ignoreCheckSignature(request)) {
            log.debug("request no signature required, enable = {}", false);
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String accessId = request.getHeader(headerNames.getAccessId());
        if (!StringUtils.hasText(accessId)) {
            badRequest(response, "request access key must not empty");
            return;
        }

        boolean signRequireBody = ApiSignatureRequest.signRequireRequestBody(request.getContentType());
        HttpServletRequest httpRequest = signRequireBody ? new RepeatableReadRequestWrapper(request) : request;
        if (isInvalidTimestamp(request.getHeader(headerNames.getTimestamp()))) {
            badRequest(response, SIGAN_VERIFY_ERROR_MESSAGE);
            return;
        }
        ApiSignatureRequest signatureRequest = buildSignatureRequest(httpRequest, signRequireBody);
        String requestSign = request.getHeader(headerNames.getSign());

        // 使用访问标识和秘钥版本号加载秘钥账号
        ApiSecretAccount account = apiSecretAccountProvider.apply(accessId, request.getHeader(headerNames.getSecretVersion()));
        if (account == null) {
            badRequest(response, String.format("please check %s, %s request header", headerNames.getAccessId(), headerNames.getSecretVersion()));
            return;
        }
        if (account.getSigner().verify(signatureRequest, account.getSecretKey(), requestSign)) {
            // 设置到签名认证账号到上下文中
            request.setAttribute(WindHttpConstants.API_SECRET_ACCOUNT_ATTRIBUTE_NAME, account);
            chain.doFilter(httpRequest, servletResponse);
            return;
        }

        if (!ServiceInfoUtils.isOnline()) {
            // 线下环境返回服务端的签名字符串，方便客户端排查签名错误
            response.addHeader(headerNames.getDebugSignContent(), signatureRequest.getSignTextForDigest());
            if (StringUtils.hasText(signatureRequest.getQueryString())) {
                response.addHeader(headerNames.getDebugSignQuery(), signatureRequest.getQueryString());
            }
        }
        log.error("sign verify error, signature request = {}", signatureRequest);
        badRequest(response, SIGAN_VERIFY_ERROR_MESSAGE);
    }

    private void badRequest(HttpServletResponse response, String message) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        HttpResponseMessageUtils.writeJson(response, RestfulApiRespFactory.badRequest(SpringI18nMessageUtils.getMessage(message)));
    }

    private boolean ignoreCheckSignature(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            // 跳过预检查请求
            return true;
        }
        // 匹配任意一个则忽略签名检查
        return ignoreRequestMatchers.stream().anyMatch(requestMatcher -> requestMatcher.matches(request));
    }

    private ApiSignatureRequest buildSignatureRequest(HttpServletRequest request, boolean requiredBody) throws IOException {
        ApiSignatureRequest.ApiSignatureRequestBuilder result = ApiSignatureRequest.builder()
                // http 请求 path，不包含查询参数和域名
                .requestPath(request.getRequestURI())
                .queryString(request.getQueryString())
                // 仅在存在查询字符串时才设置，避免获取到表单参数
                .method(request.getMethod().toUpperCase())
                // TODO 随机串的验证
                .nonce(request.getHeader(headerNames.getNonce()))
                .timestamp(request.getHeader(headerNames.getTimestamp()));
        if (requiredBody) {
            result.requestBody(StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8));
        }
        return result.build();
    }

    private boolean isInvalidTimestamp(String timestamp) {
        try {
            // 时间差值 > 有效期时间范围则无效
            return Math.abs((System.currentTimeMillis() - Long.parseLong(timestamp))) > SIGNATURE_TIMESTAMP_VALIDITY_PERIOD.get();
        } catch (NumberFormatException exception) {
            log.info("sign timestamp is invalid");
            return true;
        }
    }

    @Override
    public int getOrder() {
        return WindWebFilterOrdered.REQUEST_SIGN_FILTER.getOrder();
    }

    public interface ApiSecretAccountProvider extends BiFunction<String, String, ApiSecretAccount> {

        /**
         * @param accessId      客户端访问标识，AppId  or AccessKey
         * @param secretVersion 秘钥版本 可能为空
         * @return Api 秘钥账户列表
         */
        @Override
        ApiSecretAccount apply(String accessId, @Nullable String secretVersion);
    }
}
