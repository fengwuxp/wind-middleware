package com.wind.web.util;


import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.message.MessagePlaceholder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * web servlet file 工具类
 *
 * @author wuxp
 * @date 2025-10-15 15:27
 **/
public final class WebServletFileUtils {

    /**
     * 允许上传的文件类型
     */
    private static final Set<String> ALLOW_UPLOAD_MIME_TYPES = new CopyOnWriteArraySet<>(
            Arrays.asList(
                    MediaType.IMAGE_JPEG_VALUE,
                    MediaType.IMAGE_PNG_VALUE,
                    MediaType.IMAGE_GIF_VALUE,
                    WindMediaType.IMAGE_WEBP.toString(),
                    WindMediaType.IMAGE_SVG.toString(),
                    WindMediaType.APPLICATION_PDF.toString(),
                    // Microsoft Word (97-2003)
                    WindMediaType.MICROSOFT_WORD_2003.toString(),
                    // Microsoft Word (2007+)
                    WindMediaType.MICROSOFT_WORD.toString(),
                    // Microsoft Excel (97-2003)
                    WindMediaType.MICROSOFT_EXCEL_2003.toString(),
                    // Microsoft Excel (2007+)
                    WindMediaType.MICROSOFT_EXCEL.toString()
            )
    );

    private static final String EXCEL_EXTENSION_NAME = ".xlsx";

    private static final String WORD_EXTENSION_NAME = ".docx";

    private static final String PDF_EXTENSION_NAME = ".pdf";

    private static final String CSV_EXTENSION_NAME = ".csv";

    private static final Map<MimeType, String> MEDIA_TYPE_NAMES = Map.of(
            WindMediaType.APPLICATION_PDF, PDF_EXTENSION_NAME,
            WindMediaType.MICROSOFT_EXCEL, EXCEL_EXTENSION_NAME,
            WindMediaType.MICROSOFT_WORD, WORD_EXTENSION_NAME,
            WindMediaType.APPLICATION_CSV, CSV_EXTENSION_NAME
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
        writeFile(filename, WindMediaType.MICROSOFT_EXCEL, writeFunc);
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

    /**
     * 判断文件是否允许上传
     *
     * @param mimeType 文件类型
     * @return true 允许上传
     */
    public static boolean isAllowUpload(@NotBlank String mimeType) {
        AssertUtils.hasText(mimeType, "argument mimeType must not empty");
        return ALLOW_UPLOAD_MIME_TYPES.contains(mimeType);
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

    private static class WindMediaType {

        private static final String IMAGE = "image";

        private static final String APPLICATION = "application";

        public static final MimeType IMAGE_WEBP = new MimeType(IMAGE, "webp");

        public static final MimeType IMAGE_SVG = new MimeType(IMAGE, "svg+xml");

        public static final MimeType APPLICATION_PDF = new MimeType(APPLICATION, "pdf");

        public static final MimeType MICROSOFT_EXCEL = new MimeType(APPLICATION, "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        public static final MimeType MICROSOFT_WORD = new MimeType(APPLICATION, "vnd.openxmlformats-officedocument.wordprocessingml.document");

        public static final MimeType MICROSOFT_EXCEL_2003 = new MimeType(APPLICATION, "vnd.ms-excel");

        public static final MimeType MICROSOFT_WORD_2003 = new MimeType(APPLICATION, "msword");

        public static final MimeType APPLICATION_CSV = new MimeType(APPLICATION, "csv");

    }

}
