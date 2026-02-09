package com.wind.signature.algorithm;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.signature.SignatureAlgorithm;
import com.wind.signature.WindByteSigner;
import com.wind.signature.WindTextSigner;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;

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
 * SHAXxx With RSA 签名验证是一种常见的数字签名方法，它结合了消息摘要算法（SHA-Xxx）和非对称加密算法（RSA）。这种签名机制确保数据的完整性和来源的真实性
 *
 * @author wuxp
 * @date 2026-02-09 16:47
 **/
@AllArgsConstructor
public class ShaWithRsaByteSigner implements WindByteSigner {

    private static final String RSA_ALGORITHM_NAME = "RSA";

    private final SignatureAlgorithm algorithm;

    @Override
    public byte[] sign(byte[] signBytes, byte[] secretBytes) {
        // 构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(secretBytes);
        try {
            // 指定加密算法
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM_NAME);
            // 用私钥对信息生成数字签名
            Signature signature = Signature.getInstance(algorithm.getAlgorithm());
            signature.initSign(keyFactory.generatePrivate(keySpec));
            signature.update(signBytes);
            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException |
                 SignatureException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "sign error", exception);
        }
    }

    @Override
    public boolean verify(byte[] signBytes, byte[] secretBytes, byte[] sign) {
        // 构造X509EncodedKeySpec对象
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(secretBytes);
        try {
            // 指定加密算法
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM_NAME);
            Signature signature = Signature.getInstance(algorithm.getAlgorithm());
            signature.initVerify(keyFactory.generatePublic(keySpec));
            signature.update(signBytes);
            // 验证签名是否正常
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException |
                 SignatureException exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "verify sign error", exception);
        }
    }

    public static WindByteSigner sha256WithRsa() {
        return new ShaWithRsaByteSigner(SignatureAlgorithm.SHA256_WITH_RSA);
    }

    public static WindTextSigner sha256WithRsaBas4() {
        WindByteSigner delegate = sha256WithRsa();
        return new WindTextSigner() {
            @Override
            public @NonNull String sign(@NonNull String signText, @NonNull String secretKey) {
                byte[] bytes = delegate.sign(signText.getBytes(), Base64.getDecoder().decode(secretKey));
                return Base64.getEncoder().encodeToString(bytes);
            }

            @Override
            public boolean verify(String signText, String secretKey, String sign) {
                return delegate.verify(signText.getBytes(), Base64.getDecoder().decode(secretKey), sign.getBytes());
            }
        };
    }

}
