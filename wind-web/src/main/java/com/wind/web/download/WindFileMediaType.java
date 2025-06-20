package com.wind.web.download;

import com.wind.common.WindConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * 文件类型
 *
 * @author wuxp
 * @date 2025-06-20 17:11
 **/
@AllArgsConstructor
@Getter
public enum WindFileMediaType {

    // 图片
    IMAGE_JPEG("jpg", "image/jpeg"),
    IMAGE_JPEG2("jpeg", "image/jpeg"),
    IMAGE_PNG("png", "image/png"),
    IMAGE_GIF("gif", "image/gif"),
    IMAGE_BMP("bmp", "image/bmp"),
    IMAGE_WEBP("webp", "image/webp"),
    IMAGE_SVG("svg", "image/svg+xml"),
    IMAGE_TIFF("tiff", "image/tiff"),

    // 文档
    TEXT_PLAIN("txt", "text/plain"),
    TEXT_HTML("html", "text/html"),
    TEXT_CSS("css", "text/css"),
    APPLICATION_JSON("json", "application/json"),
    APPLICATION_XML("xml", "application/xml"),
    APPLICATION_PDF("pdf", "application/pdf"),
    APPLICATION_MSWORD("doc", "application/msword"),
    APPLICATION_DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    APPLICATION_MSEXCEL("xls", "application/vnd.ms-excel"),
    APPLICATION_XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    APPLICATION_MSPPT("ppt", "application/vnd.ms-powerpoint"),
    APPLICATION_PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    // 压缩文件
    APPLICATION_ZIP("zip", "application/zip"),
    APPLICATION_GZIP("gz", "application/gzip"),
    APPLICATION_X_TAR("tar", "application/x-tar"),
    APPLICATION_RAR("rar", "application/vnd.rar"),
    APPLICATION_7Z("7z", "application/x-7z-compressed"),

    // 音频
    AUDIO_MPEG("mp3", "audio/mpeg"),
    AUDIO_WAV("wav", "audio/wav"),
    AUDIO_OGG("ogg", "audio/ogg"),
    AUDIO_MIDI("mid", "audio/midi"),

    // 视频
    VIDEO_MP4("mp4", "video/mp4"),
    VIDEO_MPEG("mpeg", "video/mpeg"),
    VIDEO_QUICKTIME("mov", "video/quicktime"),
    VIDEO_X_MSVIDEO("avi", "video/x-msvideo"),
    VIDEO_WEBM("webm", "video/webm"),

    // 其他常见
    APPLICATION_OCTET_STREAM("bin", "application/octet-stream"),
    APPLICATION_EXE("exe", "application/x-msdownload"),
    APPLICATION_SH("sh", "application/x-sh"),
    APPLICATION_JAVA_ARCHIVE("jar", "application/java-archive");

    private final String extension;

    private final String mediaType;


    /**
     * 根据文件名获取对应的 FileMimeType 枚举
     *
     * @param filename 文件名称，包含扩展名
     * @return 对应的 FileMimeType
     */
    public static WindFileMediaType fromFilename(String filename) {
        if (filename == null || !filename.contains(WindConstants.DOT)) {
            return null;
        }
        String extension = Objects.requireNonNull(CollectionUtils.lastElement(Arrays.asList(filename.split("\\.")))).toLowerCase();
        return fromExtension(extension);
    }

    /**
     * 根据文件扩展名获取对应的 FileMimeType 枚举
     *
     * @param extension 文件扩展名（不带点），大小写不敏感
     * @return 对应的 FileMimeType
     */
    @Nullable
    public static WindFileMediaType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        for (WindFileMediaType type : values()) {
            if (Objects.equals(extension, type.extension)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据文件扩展名获取对应的 FileMimeType 枚举
     *
     * @param mediaType 内容类型
     * @return 对应的 FileMimeType
     */
    @Nullable
    public static WindFileMediaType formContentType(String mediaType) {
        if (mediaType == null || mediaType.isEmpty()) {
            return null;
        }
        for (WindFileMediaType type : values()) {
            if (Objects.equals(mediaType, type.mediaType)) {
                return type;
            }
        }
        return null;
    }
}
