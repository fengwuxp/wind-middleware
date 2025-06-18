package com.wind.websocket.core;

import com.wind.core.ObjectReadonlyMetadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * websocket 会话描述符
 *
 * @author wuxp
 * @date 2025-05-31 09:09
 **/
public interface WindSocketSessionDescriptor extends ObjectReadonlyMetadata {

    /**
     * @return 获取会话id
     */
    @NotBlank
    String getId();

    /**
     * @return 聊天会话名称
     */
    @NotBlank
    default String getName() {
        return getId();
    }

    /**
     * @return 创建时间
     */
    @NotNull
    LocalDateTime getGmtCreate();

    /**
     * @return 会话状态
     */
    WindSocketSessionStatus getStatus();

    /**
     * @return 会话连接管理策略
     */
    @NotNull
    default WindSessionConnectionPolicy getSessionConnectionPolicy() {
        return WindSessionConnectionPolicy.DEFAULT;
    }

    /**
     * @return 会话类型
     */
    default WindSocketSessionType getSessionType() {
        return WindSocketSessionType.DEFAULT;
    }
}
