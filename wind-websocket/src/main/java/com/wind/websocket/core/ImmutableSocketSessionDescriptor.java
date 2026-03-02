package com.wind.websocket.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 不可变的 socket 会话描述符
 *
 * @author wuxp
 * @date 2026-03-02 12:00
 **/
@AllArgsConstructor
@Getter
@Builder
public class ImmutableSocketSessionDescriptor implements WindSocketSessionDescriptor {

    private final String id;

    private final LocalDateTime gmtCreate;

    private final WindSocketSessionStatus status;

    private final WindSessionConnectionPolicy sessionConnectionPolicy;

    private final WindSocketSessionType sessionType;

    private final Map<String, Object> metadata;
}
