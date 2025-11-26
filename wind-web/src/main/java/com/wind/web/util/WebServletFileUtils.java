package com.wind.web.util;


import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.message.MessagePlaceholder;
import com.wind.web.http.WindMediaTypes;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MimeType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.wind.common.WindFileConstants.CSV_EXTENSION_NAME;
import static com.wind.common.WindFileConstants.EXCEL_EXTENSION_NAME;
import static com.wind.common.WindFileConstants.PDF_EXTENSION_NAME;
import static com.wind.common.WindFileConstants.WORD_EXTENSION_NAME;

/**
 * web servlet file 工具类
 *
 * @author wuxp
 * @date 2025-10-15 15:27
 **/
public final class WebServletFileUtils {


    private static final Map<MimeType, String> MEDIA_TYPE_NAMES = Map.of(
            WindMediaTypes.APPLICATION_PDF, PDF_EXTENSION_NAME,
            WindMediaTypes.MICROSOFT_EXCEL, EXCEL_EXTENSION_NAME,
            WindMediaTypes.MICROSOFT_WORD, WORD_EXTENSION_NAME,
            WindMediaTypes.APPLICATION_CSV, CSV_EXTENSION_NAME
    );

    private WebServletFileUtils() {
        throw new AssertionError();
    }

    /**
     * 响应返回 excel 文件
     *
     * @param filename  文件名
     * @param writeFunc 写回文件流的函数
     */
    public static void writeExcel(@NotBlank String filename, @NotNull Runnable writeFunc) {
        writeFile(filename, WindMediaTypes.MICROSOFT_EXCEL, writeFunc);
    }

    /**
     * 响应返回文件
     *
     * @param filename  文件名
     * @param writeFunc 写回文件流的函数
     */
    public static void writeFile(@NotBlank String filename, @NotNull Runnable writeFunc) {
        writeFile(filename, parseContentType(filename), writeFunc);
    }

    /**
     * 响应返回文件
     *
     * @param filename    文件名
     * @param contentType 文件类型
     * @param writeFunc   写回文件流的函数
     */
    public static void writeFile(@NotBlank String filename, MimeType contentType, Runnable writeFunc) {
        addFileHeaders(filename, contentType);
        writeFunc.run();
    }

    /**
     * 添加文件响应头
     *
     * @param filename    文件名
     * @param contentType 文件类型
     */
    public static void addFileHeaders(@NotBlank String filename, @Nullable MimeType contentType) {
        HttpServletResponse response = HttpServletRequestUtils.requireContextResponse();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String name = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        if (filename.contains(WindConstants.DOT)) {
            if (contentType == null) {
                // 通过文件名获取 Mime type
                contentType = parseContentType(filename);
            }
        } else {
            if (contentType != null) {
                // 通过 Mime type 获取文件扩展名
                filename = "%s%s".formatted(name, parseContentType(contentType));
            }
        }
        AssertUtils.notNull(contentType, "contentType must not null, filename = {}", filename);
        response.setContentType(contentType.toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=%s", filename));
    }

    private static String parseContentType(MimeType mediaType) {
        String result = MEDIA_TYPE_NAMES.get(mediaType);
        if (result == null) {
            throw BaseException.common(MessagePlaceholder.of("Unknown content type  {}", mediaType));
        }
        return result;
    }

    private static MimeType parseContentType(String filename) {
        AssertUtils.isTrue(filename.contains(WindConstants.DOT), "invalid filename = {}", filename);
        String lowerCase = filename.toLowerCase();
        for (Map.Entry<MimeType, String> entry : MEDIA_TYPE_NAMES.entrySet()) {
            String extensionName = entry.getValue();
            if (lowerCase.endsWith(extensionName)) {
                return entry.getKey();
            }
        }
        throw BaseException.common(MessagePlaceholder.of("Not found filename {} mime type", filename));
    }

}
