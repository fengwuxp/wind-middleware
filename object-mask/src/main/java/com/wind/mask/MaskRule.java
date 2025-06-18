package com.wind.mask;

import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.mask.masker.MaskerFactory;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 脱敏规则
 *
 * @author wuxp
 * @date 2024-08-06 12:55
 **/
@Data
public class MaskRule {

    private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

    /**
     * 需要脱敏的字段名称或表达式
     */
    @NotNull
    private final String name;

    /**
     * 在字段为 {@link java.util.Map} 类型或 json字符串等情况下用于保存需要脱敏的 keys
     * 支持正则表达式
     */
    @NotNull
    private final Set<String> keys;

    /**
     * 脱敏器
     */
    @SuppressWarnings("rawtypes")
    private final WindMasker masker;


    @SuppressWarnings("rawtypes")
    public MaskRule(String name, Collection<String> keys, WindMasker masker) {
        this.name = name;
        this.keys = keys == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(keys));
        this.masker = masker;
    }

    boolean eq(String name) {
        return Objects.equals(name, this.name);
    }

    boolean matches(String name) {
        return eq(name) || convertPattern(this.name).matcher(name).matches();
    }

    /**
     * 创建一个自定义脱敏规则
     *
     * @param fieldName 字段名称
     * @param masker    自定义的脱敏器
     * @param keys      Map 类型字段的 Keys
     */
    @SuppressWarnings("rawtypes")
    public static MaskRule mark(String fieldName, WindMasker masker, String... keys) {
        AssertUtils.notNull(fieldName, "argument fieldName must not null");
        AssertUtils.notNull(masker, "argument sanitizer must not null");
        return new MaskRule(fieldName, keys == null ? Collections.emptyList() : Arrays.asList(keys), masker);
    }

    /**
     * 创建一个脱敏规则
     *
     * @param fieldName  字段名称
     * @param maskerType 使用脱敏类型
     */
    @SuppressWarnings("rawtypes")
    public static MaskRule mark(String fieldName, Class<? extends WindMasker> maskerType) {
        return mark(fieldName, Collections.emptySet(), maskerType);
    }

    /**
     * 创建一个脱敏规则
     *
     * @param fieldName  字段名称
     * @param keys       脱敏字中需要被脱敏的 key (嵌套对象或 json 字符串)
     * @param maskerType 使用脱敏类型
     */
    @SuppressWarnings("rawtypes")
    public static MaskRule mark(String fieldName, Collection<String> keys, Class<? extends WindMasker> maskerType) {
        AssertUtils.notNull(fieldName, "argument fieldName must not null");
        AssertUtils.notNull(maskerType, "argument maskerType must not null");
        AssertUtils.notNull(keys, "argument keys must not null");
        return new MaskRule(fieldName, keys, MaskerFactory.getMasker(maskerType));
    }

    @SuppressWarnings("rawtypes")
    public static MaskRule markMap(Collection<String> keys, Class<? extends WindMasker> maskerType) {
        AssertUtils.notNull(maskerType, "argument maskerType must not null");
        AssertUtils.notNull(keys, "argument keys must not null");
        return new MaskRule(WindConstants.EMPTY, keys, MaskerFactory.getMasker(maskerType));
    }


    private static Pattern convertPattern(String value) {
        if (isRegex(value)) {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(String.format(".*%s$", value), Pattern.CASE_INSENSITIVE);
    }

    private static boolean isRegex(String value) {
        return Arrays.stream(REGEX_PARTS).anyMatch(value::contains);
    }
}