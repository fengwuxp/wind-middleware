package com.wind.server.web.download;

import com.wind.web.download.WindDownload;
import com.wind.web.download.WindFileMediaType;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 文件下载响应头处理
 *
 * @author wuxp
 * @date 2025-06-20 17:01
 **/
public class DownloadFileResponseHeaderAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, @NotNull Class converterType) {
        return returnType.hasMethodAnnotation(WindDownload.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, @NotNull MediaType selectedContentType,
                                  @NotNull Class selectedConverterType, @NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response) {
        WindDownload annotation = AnnotationUtils.findAnnotation(Objects.requireNonNull(returnType.getMethod()), WindDownload.class);
        if (annotation == null) {
            return body;
        }
        if (response instanceof ServletServerHttpResponse serverResponse) {
            String mediaType = annotation.mediaType();
            String filename = annotation.value();
            if (mediaType.isEmpty()) {
                WindFileMediaType fileMediaType = WindFileMediaType.fromFilename(filename);
                // 默认 octet-stream
                mediaType = fileMediaType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : fileMediaType.getMediaType();
            }
            HttpServletResponse servletResponse = serverResponse.getServletResponse();
            servletResponse.addHeader(HttpHeaders.CONTENT_TYPE, mediaType);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            servletResponse.addHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=%s", encodedFilename));
        }
        return body;
    }

}
