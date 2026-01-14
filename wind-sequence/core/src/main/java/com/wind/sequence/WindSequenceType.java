package com.wind.sequence;

import com.wind.common.WindConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 序列号类型，建议使用枚举实现
 *
 * @author wuxp
 * @date 2025-07-01 10:56
 **/
public interface WindSequenceType {

    /**
     * @return 序列号名称
     */
    @NotBlank
    String name();

    /**
     * @return 序列号前缀
     */
    @NotNull
    String getPrefix();

    /**
     * @return 获取序列号长度
     */
    int length();

    /**
     * 创建序列号类型
     *
     * @param name 名称
     * @return 序列号类型
     */
    static WindSequenceType immutable(String name) {
        return immutable(name, WindConstants.EMPTY, 6);
    }

    /**
     * 创建序列号类型
     *
     * @param name   名称
     * @param prefix 前缀
     * @return 序列号类型
     */
    static WindSequenceType immutable(String name, String prefix) {
        return immutable(name, prefix, 6);
    }

    /**
     * 创建序列号类型
     *
     * @param name   名称
     * @param prefix 前缀
     * @param length 长度
     * @return 序列号类型
     */
    static WindSequenceType immutable(String name, String prefix, int length) {
        return new WindSequenceType() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String getPrefix() {
                return prefix;
            }

            @Override
            public int length() {
                return length;
            }
        };
    }
}
