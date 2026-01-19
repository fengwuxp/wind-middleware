package com.wind.core.resources;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Collection;

/**
 * 资源加载器
 *
 * @author wuxp
 * @date 2024-12-12 23:45
 **/
public interface WindResourcesLoader<R extends WindResources<? extends Serializable>> {

    /**
     * 加载所有的 资源
     *
     * @return 资源列表
     */
    @NotNull
    Collection<R> loadResources();

    /**
     * 按照资源拥有者加载资源
     *
     * @param ownerId   资源用户者标识
     * @param ownerType 资源拥有者类型
     * @return owner 拥有的资源列表
     */
    @NotNull
    Collection<R> loadResources(String ownerId, String ownerType);

    /**
     * 是否支持加载该类型的资源
     *
     * @param resourcesType 资源类型
     * @return if true 支持
     */
    boolean supports(Class<R> resourcesType);
}
