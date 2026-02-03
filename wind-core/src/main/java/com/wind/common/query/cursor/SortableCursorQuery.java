package com.wind.common.query.cursor;

import com.wind.common.query.supports.QueryOrderField;

/**
 * 游标分页查询配置，一般用于控制器接收分页配置需动态指定的排序字段类型
 *
 * @author wuxp
 * @date 2026-02-03 17:30
 **/
public class SortableCursorQuery<O extends QueryOrderField> extends AbstractCursorQuery<O> {
}
