package com.wind.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户端设备类型
 * 支持操作系统 + 设备类型分类（Phone / Pad / PC / Unknown）
 *
 * @author wuxp
 * @date 2025-12-11
 */
@AllArgsConstructor
@Getter
public enum WindClientDeviceType implements DescriptiveEnum {

    UNKNOWN("Unknown", "Unknown"),

    ANDROID_PHONE("Android", "Phone"),
    ANDROID_PAD("Android", "Pad"),

    IPHONE("iOS", "Phone"),
    IPAD("iOS", "Pad"),

    WINDOWS("Windows", "PC"),
    MAC("Mac", "PC"),
    LINUX("Linux", "PC");

    /**
     * 操作系统
     */
    private final String os;

    /**
     * 设备类型分类
     */
    private final String category;

    /**
     * 获取友好描述，用于前端显示或统计
     */
    @Override
    public String getDesc() {
        return String.format("%s-%s", os, category);
    }
}
