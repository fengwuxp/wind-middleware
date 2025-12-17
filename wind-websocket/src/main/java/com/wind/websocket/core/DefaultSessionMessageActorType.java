package com.wind.websocket.core;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认消息发送者类型
 *
 * @author wuxp
 * @date 2025-12-17 09:23
 **/
@AllArgsConstructor
@Getter
public enum DefaultSessionMessageActorType implements DescriptiveEnum {

    USER("用户"),

    SYSTEM("系统"),

    ROBOT("机器人");

    private final String desc;
}
