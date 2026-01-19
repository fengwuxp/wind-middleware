package com.wind.core.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.io.Serializable;
import java.util.List;

/**
 * 树形资源
 *
 * @author wuxp
 * @date 2024-12-12 23:22
 **/
public interface TreeResources<ID extends Serializable> extends WindResources<ID> {

    /**
     * @return 父级 ID，Null 表示为根节点
     */
    @Null
    ID getParentId();

    /**
     * @return 父级名称
     */
    @Null
    String getParentName();

    /**
     * @return 资源在树形结构的层级
     */
    @NotNull
    Integer getLevel();

    /**
     * @return 子节点
     */
    @Null
    List<? extends TreeResources<ID>> getChildren();

    /**
     * @return 是否为根节点
     */
    default boolean isRoot() {
        return getParentId() == null;
    }

    /**
     * @return 是否为叶子节点
     */
    default boolean isLeaf() {
        return getChildren() == null || getChildren().isEmpty();
    }
}
