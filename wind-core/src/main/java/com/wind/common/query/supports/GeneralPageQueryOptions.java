package com.wind.common.query.supports;

import org.jspecify.annotations.Nullable;

/**
 * 通用的分页查询参数配置，支持设置排序类型
 *
 * @param <O> 排序字段类型
 * @author wuxp
 * @apiNote 仅限服务内部使用，如需要要在控制器上接收参数，请使用{@link DefaultPageQueryOptions}，或自行实现
 * @date 2025-12-09 13:46
 **/
public final class GeneralPageQueryOptions<O extends QueryOrderField> extends AbstractPageQuery<O> {

    public static <F extends QueryOrderField> GeneralPageQueryOptions<F> of() {
        return new GeneralPageQueryOptions<>();
    }

    public static <F extends QueryOrderField> GeneralPageQueryOptions<F> of(int querySize) {
        return of(1, querySize);
    }

    public static <F extends QueryOrderField> GeneralPageQueryOptions<F> of(int queryPage, int querySize) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, null, null);
    }

    public static <F extends QueryOrderField> GeneralPageQueryOptions<F> of(int queryPage, int querySize, @Nullable F[] orderFields, @Nullable QueryOrderType[] orderTypes) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, orderFields, orderTypes);
    }

    public static <F extends QueryOrderField> GeneralPageQueryOptions<F> of(int queryPage, int querySize, QueryType queryType, @Nullable F[] orderFields,
                                                                            @Nullable QueryOrderType[] orderTypes) {
        GeneralPageQueryOptions<F> result = new GeneralPageQueryOptions<>();
        result.setQueryPage(queryPage);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }
}
