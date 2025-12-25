package com.wind.common.query;

import com.wind.common.query.supports.QueryType;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.beans.Transient;
import java.io.Serializable;
import java.util.List;

/**
 * 分页对象
 *
 * @author wuxp
 * @date 2025-09-30 14:16
 **/
public interface WindPagination<T> extends Serializable {

    /**
     * @return 总记录数据, 不关心总数的场景返回 -1
     */
    long getTotal();

    /**
     * @return 数据集合列表
     */
    @NotNull
    List<T> getRecords();

    /**
     * @return 当前查询大小
     */
    int getQuerySize();

    /**
     * @return 当前查询类型
     */
    @NotNull
    QueryType getQueryType();

    /**
     * @return {@link #getRecords()} 是否为 null 或空集合
     */
    @Transient
    default boolean isEmpty() {
        return CollectionUtils.isEmpty(getRecords());
    }

    /**
     * 为了节省传输内容，该方法不参与序列化
     *
     * @return 获取第一条数据
     */
    @Transient
    @Nullable
    default T getFirst() {
        return CollectionUtils.firstElement(getRecords());
    }

}
