package com.wind.common.query.cursor;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.common.util.WindReflectUtils;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于游标分页查询的工具类
 *
 * @author wuxp
 * @date 2025-09-30 13:12
 **/
final class CursorQueryUtils {

    private static final Set<String> CURSOR_QUERY_CURSOR_FILED_NAMES = Set.of("prevCursor", "nextCursor");

    static final String CURSOR_FILED_NAME = "id";

    private CursorQueryUtils() {
        throw new AssertionError();
    }

    /**
     * 生成 cursor
     *
     * @param query 查询参数
     * @param id    数据记录的 id
     * @return cursor
     */
    @SuppressWarnings("rawtypes")
    static String generateCursor(@NotNull AbstractCursorQuery query, @NotNull Object id) {
        AssertUtils.notNull(query, "argument query must not null");
        AssertUtils.notNull(id, "argument id must not null");
        return genCursorSha256(query) + WindConstants.AT + id;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    static String checkCursorAndGetLastRecordId(@NotNull AbstractCursorQuery query, @Nullable String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = cursor.split(WindConstants.AT);
        if (parts.length != 2) {
            throw new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, "cursor 格式错误");
        }
        String signature = parts[0];
        AssertUtils.equals(genCursorSha256(query), signature, "cursor 签名错误");
        return StringUtils.hasText(parts[1]) ? parts[1] : null;
    }

    @SuppressWarnings("rawtypes")
    private static String genCursorSha256(AbstractCursorQuery query) {
        Field[] fields = WindReflectUtils.getFields(query.getClass());
        String queryString = Arrays.stream(fields)
                .filter(field -> !CURSOR_QUERY_CURSOR_FILED_NAMES.contains(field.getName()))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> field.getName() + WindConstants.EQ + WindReflectUtils.getFieldValue(field, query))
                .collect(Collectors.joining(WindConstants.AND));
        return sha256Base64(queryString);
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
