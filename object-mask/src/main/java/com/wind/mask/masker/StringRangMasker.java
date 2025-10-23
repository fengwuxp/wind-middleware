package com.wind.mask.masker;

import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import com.wind.mask.WindMasker;
import org.springframework.util.StringUtils;

/**
 * 基于内容范围的文本脱敏器
 *
 * @author wuxp
 * @date 2024-08-07 17:07
 **/
public record StringRangMasker(int begin, int end) implements WindMasker<String, String> {

    @VisibleForTesting
    static final int MAX_MASK_SIZE = 8;

    public StringRangMasker {
        AssertUtils.isTrue(begin <= end, "argument begin must lte end");
    }

    public static StringRangMasker of(int begin, int end) {
        return new StringRangMasker(begin, end);
    }

    public static StringRangMasker phone() {
        return of(3, 7);
    }

    public static StringRangMasker secret() {
        return of(-1, -1);
    }

    public static StringRangMasker idNumCn() {
        return of(4, 14);
    }

    @Override
    public String mask(String text) {
        if (StringUtils.hasText(text)) {
            int length = text.length();
            if (begin <= -1 || end <= -1) {
                // 全部脱敏
                return "******";
            }
            if (length <= begin) {
                // 不需要脱敏
                return text;
            }
            return maskText(text);
        }
        return text;
    }

    private String maskText(String input) {
        // 例：用 '*' 替换除了开头和结尾外的字符
        int length = input.length();
        if (length <= 2) {
            // 太短，不需要脱敏
            return input;
        }
        StringBuilder masked = new StringBuilder();
        // 保留开头字符
        masked.append(input, 0, begin);
        int distance = Integer.min(end, length) - begin;
        // 最多只保留 {@link MAX_MASK_SIZE} 位字符串
        int maskSize = Integer.min(distance, MAX_MASK_SIZE);
        // TODO 待优化
        masked.append("*".repeat(Math.max(0, maskSize)));
        if (end < length) {
            // 保留结尾字符
            masked.append(input, end, length);
        }
        return masked.toString();
    }
}
