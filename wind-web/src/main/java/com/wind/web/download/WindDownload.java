package com.wind.web.download;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件下载注解
 * 标记在一个方法上，表示该方法返回的内容是一个文件流
 *
 * @author wuxp
 * @date 2025-06-20 16:58
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WindDownload {

    @AliasFor("filename")
    String value() default "";

    /**
     * 文件名，建议包含文件名称和文件后缀
     *
     * @return 下载的文件名（可以包含中文）
     */
    String filename() default "";

    /**
     * 文件名称不包含扩展名或 通过{@link WindFileMediaType#fromExtension(String)} 无法获取到文件媒体类型是需要
     *
     * @return 文件内容类型
     * @see org.springframework.http.MediaType
     */
    String mediaType() default "";
}
