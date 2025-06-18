package com.wind.websocket.core;

import com.wind.common.exception.AssertUtils;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * web socket session，用于聚合一组用户，管理用户的连接，便于用户在会话内相互通信
 * 1. 一个会话中包含多个（2个以及以上）用户以及多个，在同一个会话中，一个用户可能有多个（不同的设备）连接
 * 2. 特殊情况下，会话中可能只存在一个真实用户（例如：系统通知的群组）
 * <p>
 * 用户会话连接在线策略：
 * 1. 多设备，允许多个设备同时在线，每个设备保留仅保留一个连接
 * 2. 单设备，使用旧连接
 * 3. 单设备，使用新连接
 *
 * @author wuxp
 * @date 2025-05-30 15:48
 **/
public interface WindSocketSession extends WindSocketSessionDescriptor {

    /**
     * 用户加入会话
     *
     * @param userId     用户 id
     * @param connection 连接信息，允许为空，用户可能不在线
     */
    void joinUser(@NotNull String userId, @Null WindSocketClientClientConnection connection);

    /**
     * 用户移除会话
     *
     * @param userId 用户
     */
    void removeUser(String userId);

    /**
     * 判断用户是否在会话中
     *
     * @param userId 用户id
     * @return if true 在会话中
     */
    boolean containsUser(String userId);

    /**
     * 断开连接
     *
     * @param connectionId 连接 id
     */
    void leaveConnection(@NotNull String connectionId);

    /**
     * @return 获取会话中的所有连接
     */
    @NotNull
    Collection<WindSocketClientClientConnection> getConnections();

    /**
     * @return 获取用户在会话中的所有连接
     */
    @NotNull
    List<WindSocketClientClientConnection> getUserConnections(@NotNull String userId);

    /**
     * @param userId           用户id
     * @param clientDeviceType 客户端设备标识
     * @return 获取用户在会话中的所有链接
     */
    default WindSocketClientClientConnection getUserConnectionWithDeviceType(@NotNull String userId, @NotNull String clientDeviceType) {
        AssertUtils.hasText(clientDeviceType, "argument clientDeviceType must not null");
        return getUserConnections(userId)
                .stream()
                .filter(connection -> Objects.equals(clientDeviceType, connection.getClientDeviceType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return 获取会话关联的用户id
     */
    @NotNull
    Collection<String> getUserIds();

    /**
     * 判断用户是否在线
     *
     * @param userId 用户id
     * @return if true 在线
     */
    boolean isUserOnline(@NotNull String userId);

    /**
     * 在会话中广播消息（同步发送）
     *
     * @param payload         将消息广播给所有在线用户
     * @param excludedUserIds 排除广播的用户
     */
    default void broadcastSync(@NotNull Object payload, @NotNull Collection<String> excludedUserIds) {
        broadcast(payload, excludedUserIds).join();
    }

    /**
     * 在会话中广播消息 （异步发送）
     *
     * @param payload         将消息广播给所有在线用户
     * @param excludedUserIds 排除广播的用户
     * @return 异步结果
     */
    CompletableFuture<Void> broadcast(@NotNull Object payload, @NotNull Collection<String> excludedUserIds);

    /**
     * 在会话中广播消息
     *
     * @param payload 将消息广播给所有在线用户
     */
    default void broadcast(@NotNull Object payload) {
        broadcast(payload, Collections.emptyList());
    }

}
