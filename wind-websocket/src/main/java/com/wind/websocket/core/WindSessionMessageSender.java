package com.wind.websocket.core;

import org.jspecify.annotations.NonNull;

/**
 * 会话消息发送者
 * <p>
 * 职责：
 * - 负责将 ChatMessage 投递到会话
 * - 不负责消息创建、权限校验、业务语义
 * <p>
 * 适用主体：
 * - 用户
 * - 机器人
 * - 系统
 *
 * @author wuxp
 * @date 2025-12-17 08:57
 **/
public interface WindSessionMessageSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     * @throws com.wind.common.exception.BaseException 失败则抛出异常
     */
    void sendMessage(@NonNull WindSessionMessage message);

    /**
     * 是否支持该消息
     *
     * @param sender 消息发送者
     * @return true 支持
     */
    boolean support(@NonNull WindSessionMessageActor sender);
}
