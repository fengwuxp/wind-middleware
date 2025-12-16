package com.wind.websocket.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

/**
 * 用户撤回消息指令
 *
 * @param messageId    被撤回的消息 ID
 * @param sessionId    所属会话 ID
 * @param revokeUserId 操作撤回消息的用户
 * @param revokeTime   撤回时间
 * @author wuxp
 * @date 2025-12-12 10:53
 */
@FieldNameConstants
public record ImmutableMessageRevokeCommand(@NotBlank String messageId, @NotBlank String sessionId, @NotBlank String revokeUserId, @NotNull LocalDateTime revokeTime) {

    @JsonCreator
    public ImmutableMessageRevokeCommand(
            @JsonProperty(Fields.messageId) String messageId,
            @JsonProperty(Fields.sessionId) String sessionId,
            @JsonProperty(Fields.revokeUserId) String revokeUserId,
            @JsonProperty(Fields.revokeTime) LocalDateTime revokeTime
    ) {
        this.messageId = messageId;
        this.sessionId = sessionId;
        this.revokeUserId = revokeUserId;
        this.revokeTime = revokeTime;
    }
}
