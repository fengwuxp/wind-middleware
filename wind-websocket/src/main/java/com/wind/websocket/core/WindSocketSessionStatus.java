package com.wind.websocket.core;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuxp
 * @date 2025-05-31 09:18
 **/
@AllArgsConstructor
@Getter
public enum WindSocketSessionStatus implements DescriptiveEnum {

    ACTIVE("活动中"),

    SUSPENDED("挂起"),

    ARCHIVED("已归档"),

    DELETED("已删除");

    private final String desc;
}
