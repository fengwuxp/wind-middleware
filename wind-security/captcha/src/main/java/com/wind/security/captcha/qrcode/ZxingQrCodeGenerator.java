package com.wind.security.captcha.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * 基于 google-zxing 二维码生成
 *
 * @author wuxp
 * @date 2023-09-24 13:50
 **/
@Slf4j
public class ZxingQrCodeGenerator implements QrCodeGenerator {


    @Override
    public Image generate(String content, QrCodeConfig qrCodeConfig, MatrixToImageConfig imageConfig) {
        BitMatrix qrCodeMatrix = createQrCodeMatrix(content, qrCodeConfig.getSize(), qrCodeConfig.getMargin());
        if (qrCodeMatrix == null) {
            return null;
        }
        if (StringUtils.hasLength(qrCodeConfig.getLogo())) {
            try {
                return addLogo(MatrixToImageWriter.toBufferedImage(qrCodeMatrix, imageConfig), qrCodeConfig);
            } catch (IOException exception) {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "给二维码添加图片失败", exception);
            }
        }
        return MatrixToImageWriter.toBufferedImage(qrCodeMatrix, imageConfig);
    }


    /**
     * 根据内容生成二维码数据
     *
     * @param content 二维码文字内容[为了信息安全性，一般都要先进行数据加密]
     * @param size    二维码图片宽度和高度
     * @param margin  边距
     */
    private BitMatrix createQrCodeMatrix(String content, int size, int margin) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // 设置字符编码
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        // 指定纠错等级
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, margin);
        try {
            return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (Exception exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "生成二维码图标失败", exception);
        }
    }

    /**
     * 将logo添加到二维码中间
     *
     * @param qrCodeImage  生成的二维码图片对象
     * @param qrCodeConfig 二维码配置
     */
    private BufferedImage addLogo(BufferedImage qrCodeImage, QrCodeConfig qrCodeConfig) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(qrCodeConfig.getSize(), qrCodeConfig.getSize(), BufferedImage.TYPE_INT_RGB);

        BufferedImage logoImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(qrCodeConfig.getLogo())));
        Graphics2D qrCodeImageGraphics = bufferedImage.createGraphics();

        // logo起始位置，此目的是为logo居中显示
        int logoSize = qrCodeConfig.getLogoSize();
        int x = (qrCodeConfig.getSize() - logoSize) / 2;
        int y = (qrCodeConfig.getSize() - logoSize) / 2;
        // 绘制图
        qrCodeImageGraphics.drawImage(qrCodeImage, 0, 0, qrCodeConfig.getSize(), qrCodeConfig.getSize(), null);
        qrCodeImageGraphics.drawImage(logoImage, x, y, logoSize, logoSize, null);
        qrCodeImageGraphics.drawRoundRect(x, y, logoSize, logoSize, logoSize / 10, logoSize / 10);
        // 给logo画边框 构造一个具有指定线条宽度以及 cap 和 join 风格的默认值的实心 BasicStroke
        qrCodeImageGraphics.setStroke(new BasicStroke(qrCodeConfig.getLogoBorderWith()));
        qrCodeImageGraphics.setColor(new Color(Integer.parseInt(qrCodeConfig.getLogoBorderColor().replaceFirst("#", ""), 16)));
        qrCodeImageGraphics.drawRect(x, y, logoSize, logoSize);
        qrCodeImageGraphics.dispose();
        return bufferedImage;

    }

}
