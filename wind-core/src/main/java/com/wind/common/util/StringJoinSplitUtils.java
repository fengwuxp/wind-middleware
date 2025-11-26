package com.wind.common.util;

import com.wind.common.WindConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 使用逗号分隔裁剪或连接字符串
 *
 * @author wuxp
 * @date 2023-10-23 11:05
 **/
public final class StringJoinSplitUtils {

    private StringJoinSplitUtils() {
        throw new AssertionError();
    }

    public static Set<String> split(String text) {
        if (StringUtils.hasText(text)) {
            return new HashSet<>(Arrays.asList(text.split(WindConstants.COMMA)));
        }
        return new HashSet<>();
    }

    public static Set<Long> splitAsLong(String text) {
        if (StringUtils.hasText(text)) {
            return Arrays.stream(text.split(WindConstants.COMMA)).map(Long::parseLong).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    public static <T extends Enum<?>> Set<T> splitAsEnums(String text, Class<? extends T> enumClazz) {
        if (StringUtils.hasText(text)) {
            Map<String, ? extends T> maps = Arrays.stream(enumClazz.getEnumConstants()).collect(Collectors.toMap(Enum::name, Function.identity()));
            return Arrays.stream(text.split(WindConstants.COMMA))
                    .map(maps::get)
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    @Nullable
    public static String joinEnums(@Nullable Collection<? extends Enum<?>> enums) {
        if (enums == null) {
            return null;
        }
        return join(enums.stream().map(Enum::name).toList());
    }

    @NonNull
    public static String joinEnums(@NonNull Enum<?>... enums) {
        return join(List.of(enums));
    }

    @NonNull
    public static String join(@NonNull Object... objects) {
        return join(List.of(objects));
    }

    @Nullable
    public static String join(Collection<?> texts) {
        if (texts == null) {
            return null;
        }
        return texts.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(WindConstants.COMMA));
    }
}
