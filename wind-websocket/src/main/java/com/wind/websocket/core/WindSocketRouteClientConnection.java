package com.wind.websocket.core;

import jakarta.validation.constraints.NotNull;
import java.util.concurrent.CompletableFuture;

/**
 * 远程路由连接，当 socket 是集群时，确认消息发送目标用户所在的真实节点，将消息路由到该节点。
 *
 * @author wuxp
 * @date 2025-05-30 16:17
 **/
public interface WindSocketRouteClientConnection extends WindSocketClientClientConnection {

    /**
     * 按照具体实现提供，可以是集群的内网节点 ip 或 ip:port 的组合
     *
     * @return 远程节点地址
     */
    @NotNull
    String getRemoteNodeAddress();

    /**
     * 通过 RPC 或则 http 的方式将消息转发到 {@link #getRemoteNodeAddress()} 节点
     *
     * @param payload 消息负载
     */
    @Override
    CompletableFuture<Void> send(@NotNull Object payload);
}
