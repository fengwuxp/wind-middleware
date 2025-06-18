package com.wind.common.query.supports;


import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import jakarta.validation.constraints.NotNull;
import java.beans.Transient;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页对象
 *
 * @author wuxp
 */
public interface Pagination<T> extends Serializable {

    /**
     * @return 总记录数据
     */
    long getTotal();

    /**
     * @return 数据集合列表
     */
    @NotNull
    List<T> getRecords();

    /**
     * @return 当前查询页面
     */
    int getQueryPage();

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
     * 为了节省传输内容，该方法不参与序列化
     *
     * @return 获取第一条数据
     */
    @Transient
    @Nullable
    default T getFirst() {
        return CollectionUtils.firstElement(getRecords());
    }

    /**
     * @return {@link #getRecords()}是否为 null 或空集合
     */
    @Transient
    default boolean isEmpty() {
        return CollectionUtils.isEmpty(getRecords());
    }

    static <E> Pagination<E> empty() {
        return new ImmutablePagination<>();
    }

    static <E> Pagination<E> of(List<E> records, AbstractPageQuery<?> query) {
        // 不关心总数的情况
        return of(records, query, -1);
    }

    static <E> Pagination<E> of(List<E> records, AbstractPageQuery<?> query, long total) {
        return of(records, query.getQueryPage(), query.getQuerySize(), query.getQueryType(), total);
    }

    static <E> Pagination<E> of(List<E> records, int queryPage, int querySize) {
        // 不关心总数的情况
        return of(records, queryPage, querySize, QueryType.QUERY_RESET, -1);
    }

    static <E> Pagination<E> of(List<E> records, int queryPage, int querySize, QueryType queryType, long total) {
        return new ImmutablePagination<>(total, records == null ? Collections.emptyList() : records, queryPage, querySize, queryType);
    }

}
