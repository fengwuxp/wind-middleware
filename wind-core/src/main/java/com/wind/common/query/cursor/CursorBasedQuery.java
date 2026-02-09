package com.wind.common.query.cursor;

import com.wind.common.exception.BaseException;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.query.supports.QueryOrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * 基于游标（Cursor-based）分页查询的通用接口定义。
 *
 * <p>
 * 游标分页（Cursor Pagination）是一种基于上一次查询的“游标位置”继续分页的查询方式，
 * 适用于高性能、大数据量的连续翻页场景，避免了传统 OFFSET/LIMIT 带来的性能下降与数据偏移问题。
 * </p>
 *
 * <h3>分页正确性依赖条件：</h3>
 * <ul>
 *     <li><b>稳定排序（Stable Ordering）</b>：
 *         排序字段的顺序必须确定且唯一，常用组合为 <code>创建时间 DESC, id DESC</code>。
 *         只有稳定排序才能保证游标分页的结果无重复、无遗漏。
 *     </li>
 *     <li><b>游标字段参与排序</b>：
 *         游标必须基于排序字段之一，否则分页位置无法精确定位。
 *         通常使用 <code>id</code> 或 <code>创建时间 + id</code> 作为组合排序字段。
 *     </li>
 *     <li><b>比较方向与排序方向匹配</b>：
 *         若排序方向为 <code>DESC</code>，游标比较应使用 <code>&lt;</code>；
 *         若排序方向为 <code>ASC</code>，游标比较应使用 <code>&gt;</code>。
 *         例如：
 *         <pre>
 *         ORDER BY created_at DESC, id DESC
 *         WHERE (created_at, id) &lt; (:lastCreatedAt, :lastId)
 *         </pre>
 *     </li>
 * </ul>
 *
 * <h3>游标字段类型与排序行为：</h3>
 * <ul>
 *     <li><b>数值类型（INT / BIGINT）</b>：
 *         <ul>
 *             <li>按数值大小进行排序；</li>
 *             <li>推荐用于自增主键 id；</li>
 *             <li>性能最佳，索引利用率最高。</li>
 *         </ul>
 *     </li>
 *     <li><b>字符串类型（VARCHAR / CHAR）</b>：
 *         <ul>
 *             <li>按字典序（Lexicographical Order）排序，而非数值大小；</li>
 *             <li>示例：'10' &lt; '2'（因为 '1' &lt; '2'）；</li>
 *             <li>如需数值排序，应使用 <code>CAST(id AS UNSIGNED)</code>，但可能导致索引失效；</li>
 *             <li>若为 UUID 等非连续字符串，可用于去重游标，但结果不具时间顺序语义。</li>
 *         </ul>
 *     </li>
 *     <li><b>固定长度数值字符串（如 "0001", "0002"）</b>：
 *         <ul>
 *             <li>按字典序排序结果与数值排序一致；</li>
 *             <li>可在字符串 id 场景中作为稳定游标使用。</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>示例：</h3>
 * <pre>{@code
 * // 创建时间倒序 + id 倒序分页
 * SELECT *
 * FROM orders
 * WHERE (created_at, id) < (:lastCreatedAt, :lastId)
 * ORDER BY created_at DESC, id DESC
 * LIMIT 20;
 * }</pre>
 *
 * <p>
 * 若游标字段为 id，则应确保 {@link #cursorFieldIsAcs()} 返回值与 ORDER BY 中 id 的排序方向一致。
 * </p>
 *
 * @param <OrderField> 排序字段类型枚举，需实现 {@link QueryOrderField}
 * @author wuxp
 * @date 2025-09-30 13:08
 */
public interface CursorBasedQuery<OrderField> extends WindQuery<OrderField> {

    /**
     * 获取上一页游标位置，用于获取上一页数据
     *
     * @return 上一页游标值（通常为当前查询的第一条记录对应字段的值）
     */
    @Nullable
    String getPrevCursor();

    /**
     * 获取下一页游标位置，用于获取下一页数据
     *
     * @return 下一页游标值（通常为上一次查询的最后一条记录对应字段的值）
     */
    @Nullable
    String getNextCursor();

    /**
     * 设置游标位置。
     *
     * @param prevCursor 上一页游标值（非空）
     */
    void setPrevCursor(@NotNull String prevCursor);

    /**
     * 设置游标位置。
     *
     * @param nextCursor 下一页游标值（非空）
     */
    void setNextCursor(@NotNull String nextCursor);

    /**
     * @return 排序字段数组（顺序与 {@link #getOrderTypes()} 一一对应）
     */
    @NotEmpty
    OrderField[] getOrderFields();

    /**
     * @return 排序方向数组（顺序与 {@link #getOrderFields()} 一一对应）
     */
    @NotEmpty
    QueryOrderType[] getOrderTypes();

    /**
     * 判断游标字段的排序方向是否为升序。
     * <p>
     * 若排序为 ASC，则游标分页条件应使用 <code>&gt;</code>；
     * 若排序为 DESC，则应使用 <code>&lt;</code>。
     * </p>
     *
     * @return true 表示游标字段按升序排列，否则为降序
     * @throws BaseException 若排序字段中未包含游标字段
     */
    boolean cursorFieldIsAcs();
}

