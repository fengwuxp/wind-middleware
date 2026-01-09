package com.wind.websocket.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wind.core.WritableContextVariables;
import jakarta.validation.constraints.NotBlank;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
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
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageContent(ChatMessageContentType contentType, String content, Map<String, Object> variables) implements WritableContextVariables, Serializable {

    @Serial
    private static final long serialVersionUID = 8540381989783804113L;

    @JsonCreator
    public ChatMessageContent(@JsonProperty(Fields.contentType) ChatMessageContentType contentType,
                              @JsonProperty(Fields.content) String content,
                              @JsonProperty(Fields.variables) Map<String, Object> variables) {
        this.contentType = contentType;
        this.content = content;
        this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);
    }

    public static ChatMessageContent text(String content) {
        return of(ChatMessageContentType.TEXT, content);
    }

    public static ChatMessageContent image(String content) {
        return of(ChatMessageContentType.IMAGE, content);
    }

    public static ChatMessageContent file(String content) {
        return of(ChatMessageContentType.FILE, content);
    }

    public static ChatMessageContent of(ChatMessageContentType type, String content) {
        return of(type, content, Collections.emptyMap());
    }

    public static ChatMessageContent of(ChatMessageContentType type, String content, Map<String, Object> variables) {
        return new ChatMessageContent(type, content, variables);
    }

    @Override
    @NonNull
    public WritableContextVariables putVariable(@NonNull String name, Object val) {
        variables.put(name, val);
        return this;
    }

    @Override
    @NonNull
    public WritableContextVariables removeVariable(@NonNull String name) {
        variables.remove(name);
        return this;
    }

    @Override
    @NonNull
    public Map<String, Object> getContextVariables() {
        return variables;
    }
}
