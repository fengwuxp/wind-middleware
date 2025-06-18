package com.wind.websocket.core;

import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;

/**
 * websocket 客户端连接
 *
 * @author wuxp
 * @date 2025-05-30 15:39
 **/
public interface WindSocketClientClientConnection extends SocketClientConnectionDescriptor {

    /**
     * 发送文本消息 （同步发送）
     *
     * @param message 消息内容
     */
    default void sendText(@NotNull String message) {
        sendAsync(message);
    }

    /**
     * 发送消息 （同步发送）
     *
     * @param payload 消息负载
     */
    default void sendAsync(@NotNull Object payload) {
        send(payload).join();
    }

    /**
     * 发送消息 （异步发送）
     *
     * @param payload 消息负载
     */
    CompletableFuture<Void> send(@NotNull Object payload);

    /**
     * 关闭连接
     */
    void close();

    /**
     * @return 连接是否存活
     */
    boolean isAlive();
}
