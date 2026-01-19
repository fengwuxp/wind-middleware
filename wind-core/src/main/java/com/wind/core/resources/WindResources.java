package com.wind.core.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 定义或描述，用于描述某一类型的资源对象
 *
 * @param <ID> 资源ID
 * @author wuxp
 * @date 2024-12-12 23:19
 **/
public interface WindResources<ID extends Serializable> {

    /**
     * @return 资源唯一标识
     */
    @NotNull
    ID getId();

    /**
     * @return 资源名称
     */
    @NotBlank
    String getName();

    /**
     * @return 资源排序
     */
    @NotNull
    default Integer getOrderIndex() {
        return 0;
    }
}
