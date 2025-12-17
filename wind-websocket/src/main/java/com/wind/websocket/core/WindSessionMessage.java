package com.wind.websocket.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Socket 会话消息
 *
 * @author wuxp
 * @date 2025-12-17 09:13
 **/
public interface WindSessionMessage {

    /**
     * @return 消息 id
     */
    @NotBlank
    String getId();

    /**
     * @return 消息发送者
     */
    @NotNull
    WindSessionMessageActor getSender();

    /**
     * @return 获取消息发送者标识
     */
    @NotNull
    default String getSenderId() {
        return getSender().id();
    }

    /**
     * {@link WindSocketSession#getId()} 会话 id
     *
     * @return 接收者
     */
    @NotBlank
    String getSessionId();

    /**
     * @return 发送时间戳
     */
    @NotNull
    default Long getTimestamp() {
        return getGmtCreate().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    /**
     * @return 创建时间
     */
    @NotNull
    LocalDateTime getGmtCreate();
}
