package com.wind.common.query.cursor;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.query.supports.QueryOrderType;
import com.wind.common.query.supports.QueryType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.beans.Transient;
import java.util.Objects;
import java.util.Set;

import static com.wind.common.query.cursor.CursorQueryUtils.CURSOR_FILED_NAME;

/**
 * 抽象的游标查询对象
 *
 * @author wuxp
 * @date 2025-09-30 13:12
 **/
@Data
public abstract class AbstractCursorQuery<OrderField extends QueryOrderField> implements CursorBasedQuery<OrderField> {

    /**
     * 允许的排序字段
     */
    private static final Set<String> ALLOW_ORDER_FIELDS = Set.of("id", "gmt_create");

    /**
     * 查询大小
     */
    @NotNull
    private Integer querySize = 20;

    /**
     * 查询类型
     */
    private QueryType queryType = QueryType.QUERY_RESET;

    /**
     * 排序字
     */
    private OrderField[] orderFields;

    /**
     * 排序类型
     */
    private QueryOrderType[] orderTypes;

    /**
     * 上一页游标
     */
    private String prevCursor;

    /**
     * 下一页游标
     */
    private String nextCursor;


    public OrderField[] getOrderFields() {
        AssertUtils.notEmpty(orderFields, "argument orderFields must not empty, cursor query must use {} order", CURSOR_FILED_NAME);
        for (OrderField field : orderFields) {
            AssertUtils.isTrue(ALLOW_ORDER_FIELDS.contains(field.getOrderField()), "order file must not allowed");
        }
        return orderFields;
    }

    @Nullable
    @Transient
    public String asPrevTextId() {
        return CursorQueryUtils.checkCursorAndGetLastRecordId(this, getPrevCursor());
    }

    @Nullable
    @Transient
    public String asNextTextId() {
        return CursorQueryUtils.checkCursorAndGetLastRecordId(this, getNextCursor());
    }

    @Nullable
    @Transient
    public Long asPrevNumberId() {
        String result = asPrevTextId();
        return result == null ? null : Long.parseLong(result);
    }

    @Nullable
    @Transient
    public Long asNextNumberId() {
        String result = asNextTextId();
        return result == null ? null : Long.parseLong(result);
    }

    @Override
    @Transient
    public boolean cursorFieldIsAcs() {
        for (int i = 0; i < orderFields.length; i++) {
            if (Objects.equals(orderFields[i].getOrderField(), CURSOR_FILED_NAME)) {
                return Objects.equals(orderTypes[i], QueryOrderType.ASC);
            }
        }
        throw BaseException.common("Cursor query must include sorting by " + CURSOR_FILED_NAME);
    }

    @Transient
    public boolean isFirst() {
        return prevCursor == null && nextCursor == null;
    }
}
