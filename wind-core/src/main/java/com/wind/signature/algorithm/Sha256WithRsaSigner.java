package com.wind.signature.algorithm;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


/**
 * SHA256 With RSA 签名验证是一种常见的数字签名方法，它结合了消息摘要算法（SHA-256）和非对称加密算法（RSA）。这种签名机制确保数据的完整性和来源的真实性
 *
 * @author wuxp
 * @date 2024-02-21 17:58
 **/
public final class Sha256WithRsaSigner {

    private static final String KEY_ALGORITHM = "RSA";

    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";

    private Sha256WithRsaSigner() {
        throw new AssertionError();
    }

    /**
     * 用私钥对信息生成数字签名
     *
     * @param signText   签名字符串
     * @param privateKey 私钥
     * @return 签名结果
     */
    public static String sign(String signText, String privateKey) {
        // 构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        try {
            // 指定加密算法
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            // 用私钥对信息生成数字签名
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(keyFactory.generatePrivate(keySpec));
            signature.update(signText.getBytes());
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException |
                 SignatureException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "sign error", exception);
        }
    }

    /**
     * 校验数字签名
     *
     * @param signText  签名字符串
     * @param publicKey 公钥
     * @param sign      数字签名
     * @return 签名验证是否通过
     */
    public static boolean verify(String signText, String publicKey, String sign) {
        // 构造X509EncodedKeySpec对象
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        try {
            // 指定加密算法
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(keyFactory.generatePublic(keySpec));
            signature.update(signText.getBytes());
            // 验证签名是否正常
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException |
                 SignatureException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "verify sign error", exception);
        }
    }
}
