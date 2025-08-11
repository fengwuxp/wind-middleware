package com.wind.websocket.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 不可变的聊天消息
 *
 * @author wuxp
 * @date 2025-05-30 10:59
 **/
@Builder
@Getter
@ToString
@EqualsAndHashCode
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImmutableChatMessage implements ChatMessage {

    private final String id;

    private final String fromUserId;

    private final String sessionId;

    private final List<ChatMessageContent> body;

    private final LocalDateTime gmtCreate;

    private final Long sequenceId;

    @NotNull
    private final Map<String, String> metadata;

    @JsonCreator
    public ImmutableChatMessage(
            @JsonProperty(Fields.id) String id,
            @JsonProperty(Fields.fromUserId) String fromUserId,
            @JsonProperty(Fields.sessionId) String sessionId,
            @JsonProperty(Fields.body) List<ChatMessageContent> body,
            @JsonProperty(Fields.gmtCreate) LocalDateTime gmtCreate,
            @JsonProperty(Fields.sequenceId)Long sequenceId,
            @JsonProperty(Fields.metadata) Map<String, String> metadata) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.sessionId = sessionId;
        this.body = body;
        this.gmtCreate = gmtCreate;
        this.sequenceId = sequenceId;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
}
