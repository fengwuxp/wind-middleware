package com.wind.websocket.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * socket 会话注册服务，用于维护会话状态和判断会话是否存在
 *
 * @author wuxp
 * @date 2025-12-15 17:06
 **/
public interface WindSocketSessionRegistry extends WindSocketSessionStatusOperations {

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
