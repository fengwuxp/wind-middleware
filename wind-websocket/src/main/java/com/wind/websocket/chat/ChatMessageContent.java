package com.wind.websocket.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wind.core.WritableContextVariables;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天消息内容
 *
 * @author wuxp
 * @date 2025-05-27 10:36
 **/
@Getter
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessageContent implements WritableContextVariables, Serializable {

    private static final long serialVersionUID = 8540381989783804113L;

    private final ChatMessageContentType contentType;

    private final String content;

    private final Map<String, Object> variables;

    @JsonCreator
    public ChatMessageContent(@JsonProperty(Fields.contentType) ChatMessageContentType contentType,
                              @JsonProperty(Fields.content) String content,
                              @JsonProperty(Fields.variables) Map<String, Object> variables) {
        this.contentType = contentType;
        this.content = content;
        this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);
    }

    public static ChatMessageContent of(ChatMessageContentType type, String content) {
        return of(type, content, Collections.emptyMap());
    }

    public static ChatMessageContent of(ChatMessageContentType type, String content, Map<String, Object> variables) {
        return new ChatMessageContent(type, content, variables);
    }

    @Override
    public WritableContextVariables putVariable(@NotBlank String name, Object val) {
        variables.put(name, val);
        return this;
    }

    @Override
    public WritableContextVariables removeVariable(@NotBlank String name) {
        variables.remove(name);
        return this;
    }

    @Override
    public Map<String, Object> getContextVariables() {
        return variables;
    }
}
