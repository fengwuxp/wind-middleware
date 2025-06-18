package com.wind.signature.algorithm;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * HmacSHA256 算法签名
 *
 * @author wuxp
 * @date 2023-10-18 22:08
 */
public final class HmacSHA256Signer {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSHA256Signer() {
        throw new AssertionError();
    }

    /**
     * 生成签名
     *
     * @param signText  用于生成签名的字符串
     * @param secretKey 签名秘钥
     * @return 签名内容
     */
    public static String sign(String signText, String secretKey) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            byte[] appSecretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(appSecretBytes, 0, appSecretBytes.length, ALGORITHM));
            byte[] md5Result = mac.doFinal(signText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(md5Result);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new BaseException(DefaultExceptionCode.BAD_REQUEST, "签名验失败", exception);
        }
    }
}
