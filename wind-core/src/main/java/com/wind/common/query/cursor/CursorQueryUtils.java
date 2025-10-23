package com.wind.common.query.cursor;

import com.wind.common.WindConstants;
import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.common.util.WindReflectUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于游标分页查询的工具类
 *
 * @author wuxp
 * @date 2025-09-30 13:12
 **/
final class CursorQueryUtils {

    private static final Set<String> CURSOR_QUERY_CURSOR_FILED_NAMES = Set.of("prevCursor", "nextCursor", "orderTypes");

    private static final int FIRST_PAGE_NUM = 1;

    static final String CURSOR_FILED_NAME = "id";

    private CursorQueryUtils() {
        throw new AssertionError();
    }


    static String[] generateCursors(@NotNull AbstractCursorQuery<? extends QueryOrderField> query, List<?> records) {
        boolean reachedEnd = records.size() < query.getQuerySize();
        Object first = Objects.requireNonNull(CollectionUtils.firstElement(records));
        Object last = Objects.requireNonNull(CollectionUtils.lastElement(records));

        String prevCursor = null;
        String nextCursor = null;
        int currentPage = getQueryCurrentPageNum(query);
        if (query.isFirst()) {
            // 首页
            nextCursor = reachedEnd ? null : CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last), currentPage + 1);
        } else if (query.getNextCursor() != null) {
            // 向后翻页
            prevCursor = CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, first), currentPage);
            nextCursor = reachedEnd ? null : CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last), currentPage + 1);
        } else {
            // 向前翻页
            int prevNum = currentPage - 1;
            prevCursor = (reachedEnd || prevNum == FIRST_PAGE_NUM) ? null : CursorQueryUtils.generateCursor(query,
                    WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, first), prevNum);
            nextCursor = CursorQueryUtils.generateCursor(query, WindReflectUtils.getFieldValue(CursorQueryUtils.CURSOR_FILED_NAME, last), currentPage);
        }
        return new String[]{prevCursor, nextCursor};
    }

    /**
     * 生成 cursor
     *
     * @param query   查询参数
     * @param id      数据记录的 id
     * @param pageNum 游标标记指向的页码
     * @return cursor  {sha256验签串}#{cursorId@pageNum}
     */
    static String generateCursor(@NotNull AbstractCursorQuery<? extends QueryOrderField> query, @NotNull Object id, int pageNum) {
        AssertUtils.notNull(query, "argument query must not null");
        AssertUtils.notNull(id, "argument id must not null");
        String cursorText = id + WindConstants.AT + pageNum;
        String cursor = genCursorSha256(query, cursorText) + WindConstants.SHARP + cursorText;
        return Base64.getEncoder().encodeToString(cursor.getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    static String checkCursorAndGetLastRecordId(@NotNull AbstractCursorQuery<? extends QueryOrderField> query, @Nullable String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = decodeCursorAndSplit(cursor);
        String signature = parts[0];
        String cursorText = parts[1];
        AssertUtils.equals(genCursorSha256(query, cursorText), signature, "cursor 签名错误");
        return StringUtils.hasText(cursorText) ? cursorText.split(WindConstants.AT)[0] : null;
    }

    @VisibleForTesting
    static int getQueryCurrentPageNum(AbstractCursorQuery<?> query) {
        if (query.isFirst()) {
            return FIRST_PAGE_NUM;
        }
        return query.getPrevCursor() == null ? parseCursorPageNum(query.getNextCursor()) : parseCursorPageNum(query.getPrevCursor());
    }

    @VisibleForTesting
    static int parseCursorPageNum(String cursor) {
        String[] parts = decodeCursorAndSplit(cursor);
        return Integer.parseInt(parts[1].split(WindConstants.AT)[1]);
    }

    private static String[] decodeCursorAndSplit(String cursor) {
        String[] parts = new String(Base64.getDecoder().decode(cursor)).split(WindConstants.SHARP);
        if (parts.length != 2) {
            throw new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, "cursor 格式错误");
        }
        return parts;
    }

    private static String genCursorSha256(AbstractCursorQuery<?> query, String text) {
        Field[] fields = WindReflectUtils.getFields(query.getClass());
        String queryString = Arrays.stream(fields)
                .filter(field -> !CURSOR_QUERY_CURSOR_FILED_NAMES.contains(field.getName()))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> {
                    Object value = WindReflectUtils.getFieldValue(field, query);
                    if (value == null) {
                        return WindConstants.EMPTY;
                    }
                    return field.getName() + WindConstants.EQ + queryParamValueAsText(value);
                })
                .collect(Collectors.joining(WindConstants.AND));
        return sha256Base64(queryString + WindConstants.SHARP + text);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @VisibleForTesting
    static String queryParamValueAsText(Object value) {
        if (value.getClass().isArray()) {
            if (ClassUtils.isPrimitiveArray(value.getClass())) {
                return WindArrayStringUtils.arrayToString(value);
            }
            return String.join(WindConstants.COMMA, Arrays.stream((Object[]) value).map(Object::toString).toList());
        } else if (value instanceof Collection val) {
            return String.join(WindConstants.COMMA, val.stream().map(Object::toString).toList());
        }
        return value.toString();
    }

    private static String sha256Base64(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, "SHA-256 加密失败", exception);
        }
    }
}
