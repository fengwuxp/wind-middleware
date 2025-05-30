package com.wind.common;

import java.util.Arrays;
import java.util.List;

/**
 * @author wuxp
 * @date 2023-10-18 22:08
 **/
public final class WindHttpConstants {

    private WindHttpConstants() {
        throw new AssertionError();
    }

    /**
     * http request 来源 ip
     */
    public static final String HTTP_REQUEST_IP_ATTRIBUTE_NAME = "requestSourceIp";

    /**
     * http request 来源 host
     */
    public static final String HTTP_REQUEST_HOST_ATTRIBUTE_NAME = "requestSourceHost";

    /**
     * api 请求账号
     */
    public static final String API_SECRET_ACCOUNT_ATTRIBUTE_NAME = "Wind-Attribute-Api-Secret-Account";

    /**
     * http request Host header name
     */
    public static final String HTTP_HOST_HEADER_NAME = "Host";

    /**
     * http request User-Agent header name
     */
    public static final String HTTP_USER_AGENT_HEADER_NAME = "User-Agent";

    /**
     * http request Device-Id  header name
     */
    public static final String HTTP_REQUEST_CLIENT_ID_HEADER_NAME = "Wind-Device-Id";

    /**
     * 线下环境给 client 响应服务端真实 IP 方便定位问题
     */
    public static final String REAL_SERVER_IP = "Real-Server-Ip";

    /**
     * client 真实 ip 头名称
     */
    public static final List<String> CLIENT_IP_HEAD_NAMES = Arrays.asList(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "X-Real-IP",
            "REMOTE-HOST",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    /**
     * 匹配所有路径的 Ant pattern
     */
    public static final String ALL_PATH_ANT_PATTERN = "/**";

    /**
     * 请求异常日志输出标记
     */
    public static String getRequestExceptionLogOutputMarkerAttributeName(Throwable throwable) {
        return String.format("%s-Log-Output-Marker", throwable.getClass().getName());
    }
}
