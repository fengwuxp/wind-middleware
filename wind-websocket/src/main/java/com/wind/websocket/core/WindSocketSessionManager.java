package com.wind.websocket.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.Map;

/**
 * socket 会话管理服务
 *
 * @author wuxp
 * @date 2025-05-30 10:41
 **/
public interface WindSocketSessionManager extends WindSocketSessionStatusOperations {

    /**
     * 创建会话
     *
     * @return 会话
     */
    default WindSocketSession createSession() {
        return createSession(null);
    }

    /**
     * 创建会话
     *
     * @param name 会话名称
     * @return 会话
     */
    default WindSocketSession createSession(@Null String name) {
        return createSession(null, name);
    }

    /**
     * 创建会话
     *
     * @param sessionId 指定的会话 id
     * @param name      会话名称
     * @return 会话
     */
    default WindSocketSession createSession(@Null String sessionId, @Null String name) {
        return createSession(sessionId, name, null);
    }

    /**
     * 创建会话
     *
     * @param sessionId 指定的会话 id
     * @param name      会话名称
     * @param metadata  会话元数据
     * @return 会话
     */
    WindSocketSession createSession(@Null String sessionId, @Null String name, Map<String, Object> metadata);

    /**
     * 更新会话名称
     *
     * @param sessionId 会话 id
     * @param newName   新的会话名称
     */
    void updateSessionName(@NotBlank String sessionId, @NotBlank String newName);

    /**
     * 获取会话
     *
     * @param sessionId 会话 id
     * @return 会话信息
     */
    @NotNull
    WindSocketSession getSession(@NotBlank String sessionId);

    /**
     * 判断会话是否存在
     *
     * @param sessionId 会话 id
     * @return 是否存在
     */
    boolean exists(@NotBlank String sessionId);
}
