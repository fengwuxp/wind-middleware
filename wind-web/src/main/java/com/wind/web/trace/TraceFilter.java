package com.wind.web.trace;

import com.wind.common.WindConstants;
import com.wind.common.util.IpAddressUtils;
import com.wind.common.util.ServiceInfoUtils;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.trace.WindTracer;
import com.wind.web.exception.GlobalExceptionLogDecisionMaker;
import com.wind.web.util.HttpResponseMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wind.common.WindConstants.HTTP_REQUEST_URL_TRACE_NAME;
import static com.wind.common.WindConstants.LOCAL_HOST_IP_V4;
import static com.wind.common.WindConstants.WIND_TRANCE_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_HOST_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_CLIENT_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_HOST_ATTRIBUTE_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;
import static com.wind.common.WindHttpConstants.HTTP_USER_AGENT_HEADER_NAME;

/**
 * trace filter
 *
 * @author wuxp
 * @date 2023-10-18 22:12
 **/
@Slf4j
public class TraceFilter extends OncePerRequestFilter {

    /**
     * 线下环境给 client 响应服务端真实 IP 方便定位问题
     */
    private static final String REAL_SERVER_IP = "Real-Server-Ip";

    /**
     * client 真实 ip 头名称
     */
    private static final List<String> CLIENT_IP_HEAD_NAMES = Arrays.asList(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "X-Real-IP",
            "REMOTE-HOST",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) {
        try {
            if (!ServiceInfoUtils.isOnline()) {
                // 线下环境增加服务端 ip 返回
                response.setHeader(REAL_SERVER_IP, IpAddressUtils.getLocalIpv4WithCache());
            }
            // 提前写入 traceId 到响应头，避免 response committed 后无法写回
            response.setHeader(WIND_TRANCE_ID_HEADER_NAME, trace(request));
            chain.doFilter(request, response);
        } catch (Throwable throwable) {
            // 统一错误捕获
            if (GlobalExceptionLogDecisionMaker.requiresPrintErrorLog(throwable)) {
                // 表明该异常未输出过 error 日志
                log.error("request error, cause by = {}", throwable.getMessage(), throwable);
            }
            HttpResponseMessageUtils.writeApiResp(response, RestfulApiRespFactory.withThrowable(throwable));
        } finally {
            WindTracer.TRACER.clear();
        }
    }

    private String trace(HttpServletRequest request) {
        String traceId = request.getHeader(WIND_TRANCE_ID_HEADER_NAME);
        Map<String, Object> contextVariables = new HashMap<>();
        // 将用户请求来源 ip 并设置到请求上下文中
        String requestSourceIp = getRequestSourceIp(request);
        request.setAttribute(HTTP_REQUEST_IP_ATTRIBUTE_NAME, requestSourceIp);
        contextVariables.put(HTTP_REQUEST_IP_ATTRIBUTE_NAME, requestSourceIp);
        contextVariables.put(HTTP_REQUEST_HOST_ATTRIBUTE_NAME, getRequestSourceHost(request));
        contextVariables.put(HTTP_REQUEST_URL_TRACE_NAME, request.getRequestURI());
        contextVariables.put(HTTP_USER_AGENT_HEADER_NAME, request.getHeader(HTTP_USER_AGENT_HEADER_NAME));
        contextVariables.put(HTTP_REQUEST_CLIENT_ID_HEADER_NAME, request.getHeader(HTTP_REQUEST_CLIENT_ID_HEADER_NAME));
        contextVariables.put(LOCAL_HOST_IP_V4, IpAddressUtils.getLocalIpv4WithCache());
        WindTracer.TRACER.trace(traceId, contextVariables);
        return WindTracer.TRACER.getTraceId();
    }

    /**
     * 获取请求来源 IP 地址, 如果通过代理进来，则透过防火墙获取真实IP地址
     *
     * @param request 请求对象
     * @return 真实 ip
     */
    private String getRequestSourceIp(HttpServletRequest request) {
        for (String headName : CLIENT_IP_HEAD_NAMES) {
            String ip = request.getHeader(headName);
            if (ip == null || ip.trim().isEmpty()) {
                continue;
            }
            ip = ip.trim();
            // 对于通过多个代理的情况， 第一个 ip 为客户端真实IP,多个IP按照 ',' 分隔
            String[] sections = ip.split(WindConstants.COMMA, 2);
            return sections[0];
        }
        return request.getRemoteAddr();
    }

    private String getRequestSourceHost(HttpServletRequest request) {
        try {
            String host = request.getHeader(HTTP_HOST_HEADER_NAME);
            return host == null ? URI.create(request.getRequestURI()).getHost() : host;
        } catch (Exception exception) {
            log.error("get request host error, request url = {}", request.getRequestURL());
            return WindConstants.UNKNOWN;
        }
    }
}
