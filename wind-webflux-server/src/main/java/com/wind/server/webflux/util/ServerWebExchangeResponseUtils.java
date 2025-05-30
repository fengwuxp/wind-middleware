package com.wind.server.webflux.util;

import com.alibaba.fastjson2.JSON;
import com.wind.server.web.supports.ApiResp;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author wuxp
 * @date 2025-05-29 14:12
 **/
public final class ServerWebExchangeResponseUtils {


    private ServerWebExchangeResponseUtils() {
        throw new AssertionError();
    }

    /**
     * 响应返回 json 数据
     *
     * @param response http response
     * @param resp     响应
     */
    public static void writeApiResp(ServerWebExchange response, ApiResp<?> resp) {
        if (resp.getHttpStatus() != null) {
            response.getResponse().setStatusCode(resp.getHttpStatus());
        }
        writeJsonText(response.getResponse(), JSON.toJSONString(resp));
    }

    /**
     * 响应返回 json 数据
     *
     * @param response http response
     * @param data     响应数据
     */
    public static void writeJson(ServerHttpResponse response, Object data) {
        writeJsonText(response, JSON.toJSONString(data));
    }

    /**
     * 响应返回 json数据
     * 注意该方法调用后会关闭响应流
     *
     * @param response http response
     * @param data     响应数据
     */
    public static void writeJsonText(ServerHttpResponse response, String data) {
        if (response.isCommitted()) {
            return;
        }
        if (response.getStatusCode() == null) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))).subscribe();
    }
}
