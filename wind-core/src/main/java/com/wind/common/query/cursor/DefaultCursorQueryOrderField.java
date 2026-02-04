package com.wind.common.query.cursor;

import com.wind.common.query.supports.QueryOrderField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * 游标查询排序字段
 *
 * @author wuxp
 * @date 2025-12-09 13:41
 **/
@AllArgsConstructor
@Getter
public enum DefaultCursorQueryOrderField implements QueryOrderField {

    /**
     * ID
     */
    ID("id"),

    /**
     * 创建日期
     */
    GMT_CREATE("gmt_create");

    /**
     * 排序字段
     */
    private final String orderField;

    /**
     * 通过字段名称获取排序字段
     *
     * @param orderField 排序字段
     * @return 排序字段
     */
    @Nullable
    public static DefaultCursorQueryOrderField valueWithOrderField(@Nullable String orderField) {
        if (orderField == null) {
            return null;
        }
        String normalized = orderField.trim().toLowerCase();
        for (DefaultCursorQueryOrderField value : values()) {
            if (value.orderField.equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}