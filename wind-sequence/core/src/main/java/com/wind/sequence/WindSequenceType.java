package com.wind.sequence;

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
     * @return 序列号默认长度
     */
    int defaultLength();
}
