package com.wind.websocket.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.experimental.FieldNameConstants;

/**
 * 会话消息发送者，可以是用户、系统、机器人等
 *
 * @author wuxp
 * @date 2025-12-17 09:22
 **/
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public record WindSessionMessageActor(@NotNull String id, @Null String name, @NotNull DefaultSessionMessageActorType type) {

    @JsonCreator
    public WindSessionMessageActor(@JsonProperty(Fields.id) String id,
                                   @JsonProperty(Fields.name) @Null String name,
                                   @JsonProperty(Fields.type) DefaultSessionMessageActorType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public static WindSessionMessageActor ofUser(String id) {
        return ofUser(id, id);
    }

    public static WindSessionMessageActor ofUser(String id, String name) {
        return new WindSessionMessageActor(id, name, DefaultSessionMessageActorType.USER);
    }

    public static WindSessionMessageActor ofRobot(String id) {
        return ofRobot(id, DefaultSessionMessageActorType.ROBOT.name());
    }

    public static WindSessionMessageActor ofRobot(String id, String name) {
        return new WindSessionMessageActor(id, name, DefaultSessionMessageActorType.ROBOT);
    }

    public static WindSessionMessageActor ofSystem(String id) {
        return ofSystem(id, DefaultSessionMessageActorType.SYSTEM.name());
    }

    public static WindSessionMessageActor ofSystem(String id, String name) {
        return new WindSessionMessageActor(id, name, DefaultSessionMessageActorType.SYSTEM);
    }

    public boolean isUser() {
        return DefaultSessionMessageActorType.USER.equals(type);
    }

    public boolean isRobot() {
        return DefaultSessionMessageActorType.ROBOT.equals(type);
    }

    public boolean isSystem() {
        return DefaultSessionMessageActorType.SYSTEM.equals(type);
    }

}
