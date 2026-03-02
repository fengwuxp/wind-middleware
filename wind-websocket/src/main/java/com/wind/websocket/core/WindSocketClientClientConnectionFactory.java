package com.wind.websocket.core;

import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * socket 连接工厂
 *
 * @author wuxp
 * @date 2026-03-02 13:10
 **/
public interface WindSocketClientClientConnectionFactory {


    /**
     * 创建 socket 连接
     *
     * @param connectionInstance 连接实例
     * @param sessionId          会话id
     * @param metadata           连接元数据
     * @return 连接实例
     */
    @NonNull
    WindSocketClientClientConnection create(@NonNull Object connectionInstance, @NonNull String sessionId, @NonNull Map<String, Object> metadata);
}
