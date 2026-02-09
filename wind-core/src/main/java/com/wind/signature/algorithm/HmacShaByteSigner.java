package com.wind.signature.algorithm;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.signature.SignatureAlgorithm;
import com.wind.signature.SimpleTextSignerWrapper;
import com.wind.signature.WindByteSigner;
import com.wind.signature.WindTextSigner;
import lombok.AllArgsConstructor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * HmacSHA 签名
 *
 * @author wuxp
 * @date 2026-02-09 16:11
 **/
@AllArgsConstructor
public final class HmacShaByteSigner implements WindByteSigner {

    private final SignatureAlgorithm algorithm;

    /**
     * 生成签名
     *
     * @param signBytes   用于生成签名的内容
     * @param secretBytes 签名秘钥
     * @return 签名内容的二进制编码
     */
    @Override
    public byte[] sign(byte[] signBytes, byte[] secretBytes) {
        try {
            Mac mac = Mac.getInstance(algorithm.getAlgorithm());
            mac.init(new SecretKeySpec(secretBytes, algorithm.getAlgorithm()));
            return mac.doFinal(signBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new BaseException(DefaultExceptionCode.BAD_REQUEST, "签名验失败", exception);
        }
    }

    @Override
    public boolean verify(byte[] signBytes, byte[] secretBytes, byte[] sign) {
        return Arrays.equals(sign(signBytes, secretBytes), sign);
    }

    public static WindByteSigner hmacSha256() {
        return new HmacShaByteSigner(SignatureAlgorithm.HMAC_SHA256);
    }

    public static WindByteSigner hmacSha512() {
        return new HmacShaByteSigner(SignatureAlgorithm.HMAC_SHA512);
    }

    public static WindTextSigner hmacSha256Base64() {
        return SimpleTextSignerWrapper.base64(hmacSha256());
    }

    public static WindTextSigner hmacSha512Base64() {
        return SimpleTextSignerWrapper.base64(hmacSha512());
    }

}