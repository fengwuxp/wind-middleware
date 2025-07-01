package com.wind.websocket.core;

import jakarta.validation.constraints.NotBlank;

/**
 * 会话状态操作
 *
 * @author wuxp
 * @date 2025-07-01 14:32
 **/
public interface WindSocketSessionStatusOperations {

    /**
     * 激活会话
     *
     * @param sessionId 会话 id
     */
    void activeSession(@NotBlank String sessionId);

    /**
     * 挂起一个会话
     *
     * @param sessionId 会话 id
     */
    void suspendSession(@NotBlank String sessionId);

    /**
     * 销毁一个会话
     *
     * @param sessionId 会话 id
     */
    void destroySession(@NotBlank String sessionId);

}
