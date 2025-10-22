package com.wind.common.util;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件 MimeType 检测工具类
 * 参见：https://zh.wikipedia.org/zh-cn/%E4%BA%92%E8%81%94%E7%BD%91%E5%AA%92%E4%BD%93%E7%B1%BB%E5%9E%8B?utm_source=chatgpt.com
 *
 * @author wuxp
 * @date 2025-10-22 13:51
 **/
@Slf4j
public final class FileMimeTypeDetectUtils {

    private static final Tika TIKA = new Tika();

    private FileMimeTypeDetectUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static MimeType detectAs(@NotBlank String filepath) {
        return MimeType.valueOf(detect(filepath));
    }

    @NotNull
    public static MimeType detectAs(@NotNull Path path) {
        return MimeType.valueOf(detect(path));
    }

    @NotNull
    public static MimeType detectAs(@NotNull File file) {
        return MimeType.valueOf(detect(file));
    }

    @NotNull
    public static MimeType detectAs(@NotNull InputStreamSource source) {
        return MimeType.valueOf(detect(source));
    }

    @NotNull
    public static MimeType detectAs(@NotNull InputStream in, @Nullable String filename) {
        return MimeType.valueOf(detect(in, filename));
    }

    @NotNull
    public static MimeType detectAs(@NotNull InputStreamSource source, @Nullable String filename) {
        return MimeType.valueOf(detect(source, filename));
    }

    @NotBlank
    public static String detect(@NotBlank String filepath) {
        return detect(new File(filepath));
    }

    @NotBlank
    public static String detect(@NotNull Path filepath) {
        return detect(filepath.toFile());
    }

    @NotBlank
    public static String detect(@NotNull File file) {
        return detect(() -> Files.newInputStream(file.toPath()), file.getName());
    }

    @NotBlank
    public static String detect(@NotNull InputStreamSource source) {
        return detect(source, null);
    }

    @NotBlank
    public static String detect(@NotNull InputStream in, @Nullable String filename) {
        return detect(() -> in, filename);
    }

    /**
     * 检测文件类型
     *
     * @param source   输入流
     * @param filename 文件名
     * @return mime 文件类型
     */
    @NotBlank
    public static String detect(@NotNull InputStreamSource source, @Nullable String filename) {
        try (InputStream in = source.getInputStream()) {
            if (StringUtils.hasText(filename)) {
                return TIKA.detect(in, filename);
            } else {
                return TIKA.detect(in);
            }
        } catch (IOException exception) {
            log.error("detect mimeType exception, filename = {}, message = {}", filename, exception.getMessage(), exception);
            throw new BaseException(DefaultExceptionCode.COMMON_FRIENDLY_ERROR, "detect mimeType exception", exception);
        }
    }
}
