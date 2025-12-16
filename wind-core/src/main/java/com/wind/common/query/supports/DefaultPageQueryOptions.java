package com.wind.common.query.supports;

import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 默认的分页查询参数配置
 *
 * @author wuxp
 * @date 2025-12-09 13:46
 **/
public final class DefaultPageQueryOptions extends AbstractPageQuery<DefaultOrderField> {

    /**
     * 降序
     *
     * @param field 排序字段
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions desc(@NonNull DefaultOrderField field) {
        return order(field, QueryOrderType.DESC);
    }

    /**
     * 升序
     *
     * @param field 排序字段
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions asc(@NonNull DefaultOrderField field) {
        return order(field, QueryOrderType.ASC);
    }

    /**
     * 排序
     *
     * @param field     排序字段
     * @param orderType 排序类型
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions order(@NonNull DefaultOrderField field, @NonNull QueryOrderType orderType) {
        return defaults(1, 20, field, orderType);
    }

    /**
     * 仅返回结果集
     *
     * @param querySize 查询数量
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions result(int querySize) {
        return result(1, querySize);
    }

    /**
     * 仅返回结果集
     *
     * @param queryPage 查询页码
     * @param querySize 查询数量
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions result(int queryPage, int querySize) {
        return result(queryPage, querySize, null, null);
    }

    /**
     * 仅返回结果集
     *
     * @param queryPage  查询页码
     * @param querySize  查询数量
     * @param orderField 排序字段
     * @param orderType  排序类型
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions result(int queryPage, int querySize, @Nullable DefaultOrderField orderField, @Nullable QueryOrderType orderType) {
        return of(queryPage, querySize, QueryType.QUERY_RESET,
                orderField == null ? null : new DefaultOrderField[]{orderField},
                orderType == null ? null : new QueryOrderType[]{orderType}
        );
    }

    /**
     * 仅统计总数
     *
     * @return DefaultPageQueryOptions
     */
    public static DefaultPageQueryOptions count() {
        return count(null, null);
    }

    /**
     * 仅统计总数
     *
     * @param orderField 排序字段
     * @param orderType  排序类型
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions count(@Nullable DefaultOrderField orderField, @Nullable QueryOrderType orderType) {
        return of(1, 20, QueryType.COUNT_TOTAL,
                orderField == null ? null : new DefaultOrderField[]{orderField},
                orderType == null ? null : new QueryOrderType[]{orderType}
        );
    }

    @NonNull
    public static DefaultPageQueryOptions defaults() {
        return new DefaultPageQueryOptions();
    }

    @NonNull
    public static DefaultPageQueryOptions defaults(int querySize) {
        return defaults(1, querySize);
    }

    @NonNull
    public static DefaultPageQueryOptions defaults(int queryPage, int querySize) {
        return of(queryPage, querySize, QueryType.QUERY_BOTH, null, null);
    }

    /**
     * 默认的分页查询参数配置
     *
     * @param queryPage  查询页码
     * @param querySize  查询数量
     * @param orderField 排序字段
     * @param orderType  排序类型
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions defaults(int queryPage, int querySize, @Nullable DefaultOrderField orderField, @Nullable QueryOrderType orderType) {
        return of(queryPage, querySize,
                QueryType.QUERY_BOTH,
                orderField == null ? null : new DefaultOrderField[]{orderField},
                orderType == null ? null : new QueryOrderType[]{orderType}
        );
    }

    /**
     * 默认的分页查询参数配置
     *
     * @param queryPage   查询页码
     * @param querySize   查询数量
     * @param queryType   查询类型
     * @param orderFields 排序字段
     * @param orderTypes  排序类型
     * @return DefaultPageQueryOptions
     */
    @NonNull
    public static DefaultPageQueryOptions of(int queryPage, int querySize, QueryType queryType, @Nullable DefaultOrderField[] orderFields, @Nullable QueryOrderType[] orderTypes) {
        AssertUtils.isTrue(queryPage > 0, "queryPage must ge 0");
        AssertUtils.isTrue(querySize > 0, "querySize must ge 0");
        DefaultPageQueryOptions result = new DefaultPageQueryOptions();
        result.setQueryPage(queryPage);
        result.setQuerySize(querySize);
        result.setQueryType(queryType);
        result.setOrderFields(orderFields);
        result.setOrderTypes(orderTypes);
        return result;
    }
}
