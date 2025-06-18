package com.wind.security.captcha.picture;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 图片生成器
 *
 * @author wuxp
 * @date 2023-09-24 12:46
 **/
public interface PictureGenerator {

    String PNG_IMAGE_FORMAT = "png";

    /**
     * generate picture
     *
     * @param content 图片内容
     * @param width   图片高度
     * @param height  图片宽度
     * @return 图片
     */
    Image generate(String content, int width, int height);

    /**
     * generate picture
     *
     * @param content 图片内容
     * @param width   图片高度
     * @param height  图片宽度
     * @return 图片
     */
    default String generateAndAsBas64(String content, int width, int height) {
        return generateAndAsBas64(content, width, height, PNG_IMAGE_FORMAT);
    }

    /**
     * generate picture
     *
     * @param content 图片内容
     * @param width   图片高度
     * @param height  图片宽度
     * @return 图片
     */
    default String generateAndAsBas64(String content, int width, int height, String format) {
        return asBase64(generate(content, width, height), format);
    }

    /**
     * 图片转 base64 编码
     *
     * @param image  图片
     * @param format 图片格式
     * @return 图片 base64 编码
     */
    default String asBase64(Image image, String format) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write((BufferedImage) image, format, output);
            // convert base64
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "图片转 base64 编码失败", exception);
        }
    }

}
