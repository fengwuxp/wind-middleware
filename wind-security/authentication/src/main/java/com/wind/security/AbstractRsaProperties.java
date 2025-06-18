package com.wind.security;

import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import lombok.Getter;
import lombok.Setter;

import java.beans.Transient;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author wuxp
 * @date 2023-11-02 17:36
 **/
@Getter
@Setter
public abstract class AbstractRsaProperties {

    /**
     * rsa 公钥
     */
    private String rsaPublicKey;

    /**
     * rsa 私钥
     */
    private String rsaPrivateKey;


    @Transient
    public KeyPair getKeyPair() {
        try {
            return new KeyPair(getPublicKey(), getPrivateKey());
        } catch (Exception exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "生成 Rsa 秘钥对失败", exception);
        }
    }

    private PublicKey getPublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(getRsaPublicKey());
        return getRsaFactory().generatePublic(new X509EncodedKeySpec(bytes));
    }

    private PrivateKey getPrivateKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(getRsaPrivateKey());
        return getRsaFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static KeyFactory getRsaFactory() throws NoSuchAlgorithmException {
        return KeyFactory.getInstance("RSA");
    }
}
