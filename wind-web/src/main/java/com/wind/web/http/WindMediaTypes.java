package com.wind.web.http;

import org.springframework.util.MimeType;

/**
 * web 媒体类型
 *
 * @author wuxp
 * @date 2025-10-17 10:29
 **/
public final class WindMediaTypes {

    private WindMediaTypes() {
        throw new AssertionError();
    }

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