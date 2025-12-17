package com.wind.websocket.command;

import com.wind.websocket.core.WindSessionMessage;
import com.wind.websocket.core.WindSessionMessageActor;
import com.wind.websocket.core.WindSocketSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话状态变更命令
 *
 * @author wuxp
 * @date 2025-12-17 16:34
 **/
@AllArgsConstructor
@Getter
public class ImmutableSessionStatusChangedCommand implements WindSessionMessage {

    private final String id;

    private final String sessionId;

    private final WindSessionMessageActor sender;

    private final WindSocketSessionStatus status;

    private final LocalDateTime gmtCreate;

    public static ImmutableSessionStatusChangedCommand active(String sessionId) {
        return of(sessionId, WindSocketSessionStatus.ACTIVE);
    }

    public static ImmutableSessionStatusChangedCommand suspended(String sessionId) {
        return of(sessionId, WindSocketSessionStatus.SUSPENDED);
    }

    public static ImmutableSessionStatusChangedCommand of(String sessionId, WindSocketSessionStatus status) {
        return new ImmutableSessionStatusChangedCommand(UUID.randomUUID().toString(), sessionId, WindSessionMessageActor.ofSystem("system"),
                status, LocalDateTime.now());
    }
}
