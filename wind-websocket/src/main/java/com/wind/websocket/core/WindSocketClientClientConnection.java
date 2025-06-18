package com.wind.websocket.core;

import jakarta.validation.constraints.NotNull;

/**
 * websocket 客户端连接
 *
 * @author wuxp
 * @date 2025-05-30 15:39
 **/
public interface WindSocketClientClientConnection extends SocketClientConnectionDescriptor {

    /**
     * 发送文本消息
     *
     * @param message 消息内容
     */
    default void sendText(@NotNull String message) {
        send(message);
    }

    /**
     * 发送消息
     *
     * @param payload 消息负载
     */
    void send(@NotNull Object payload);

    /**
     * 关闭连接
     */
    void close();

    /**
     * @return 连接是否存活
     */
    boolean isAlive();
}
