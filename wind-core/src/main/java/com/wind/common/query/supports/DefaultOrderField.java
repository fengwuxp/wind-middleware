package com.wind.common.query.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 默认查询排序字段
 *
 * @author wuxp
 * @since 2023-10-21
 */
@AllArgsConstructor
@Getter
public enum DefaultOrderField implements QueryOrderField {

    /**
     * ID
     */
    ID("id"),

    /**
     * 创建日期
     */
    GMT_CREATE("gmt_create"),

    /**
     * 编辑日期
     */
    GMT_MODIFIED("gmt_modified"),

    /**
     * 排序
     */
    ORDER_INDEX("order_index"),

    /**
     * 优先级
     */
    PRIORITY("priority"),

    /**
     * 状态
     */
    STATE("state"),

    /**
     * 是否启用
     */
    ENABLED("is_enabled"),

    /**
     * 完成时间
     */
    FINISHED_TIME("finished_time"),

    /**
     * 完成时间
     */
    COMPLETED_TIME("completed_time");

    /**
     * 排序字段
     */
    private final String orderField;

    private static final DefaultOrderField[] CREATE_ORDER_FIELDS = QueryOrderField.of(DefaultOrderField.GMT_CREATE);

    private static final DefaultOrderField[] MODIFIED_ORDER_FIELDS = QueryOrderField.of(DefaultOrderField.GMT_MODIFIED);

    /**
     * @return 返回按照创建时间排序
     */
    public static DefaultOrderField[] gmtCreate() {
        return CREATE_ORDER_FIELDS;
    }

    /**
     * @return 返回按照更新时间排序
     */
    public static DefaultOrderField[] gmtModified() {
        return MODIFIED_ORDER_FIELDS;
    }
}
