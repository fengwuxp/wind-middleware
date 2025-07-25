package com.wind.web.util;

import com.wind.trace.WindTracer;

import jakarta.annotation.Nullable;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_CLIENT_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;

/**
 * http trace variable 变量获取
 *
 * @author wuxp
 * @date 2023-12-29 10:58
 **/
public final class HttpTraceVariableUtils {

    private HttpTraceVariableUtils() {
        throw new AssertionError();
    }

    /**
     * @return 获取请求来源 ip
     */
    @Nullable
    public static String getRequestSourceIp() {
        return WindTracer.TRACER.getContextVariable(HTTP_REQUEST_IP_ATTRIBUTE_NAME);
    }

    /**
     * @return 获取请求客户端设备标识
     */
    @Nullable
    public static String getRequestDeviceId() {
        return WindTracer.TRACER.getContextVariable(HTTP_REQUEST_CLIENT_ID_HEADER_NAME);
    }
}
