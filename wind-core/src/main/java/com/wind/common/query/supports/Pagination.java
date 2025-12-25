package com.wind.common.query.supports;


import com.wind.common.query.WindPagination;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * 分页对象
 *
 * @author wuxp
 */
public interface Pagination<T> extends WindPagination<T> {

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


    static <E> Pagination<E> empty() {
        return new ImmutablePagination<>();
    }

    static <E> Pagination<E> of(List<E> records, AbstractPageQuery<? extends QueryOrderField> query) {
        // 不关心总数的情况
        return of(records, query, -1);
    }

    static <E> Pagination<E> of(List<E> records, AbstractPageQuery<? extends QueryOrderField> query, long total) {
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
