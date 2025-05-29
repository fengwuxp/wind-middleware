package com.wind.security.authentication;

import com.wind.security.authentication.jwt.JwtProperties;
import com.wind.security.authentication.jwt.JwtTokenCodec;
import org.springframework.util.Base64Utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * @author wuxp
 * @date 2025-05-29 10:08
 **/
public strictfp class JwtTokenCodecHelper {

    private JwtTokenCodecHelper() {
        throw new AssertionError();
    }

    public static JwtTokenCodec createCodec(Duration duration) {
        return createCodec(jwtProperties(duration));
    }

    public static JwtTokenCodec createCodec(JwtProperties properties) {
        return JwtTokenCodec.builder()
                .issuer(properties.getIssuer())
                .audience(properties.getAudience())
                .effectiveTime(properties.getEffectiveTime())
                .refreshEffectiveTime(properties.getRefreshEffectiveTime())
                .rsaKeyPair(properties.getKeyPair())
                .build();
    }


    public static JwtProperties jwtProperties(Duration duration) {
        KeyPair keyPair = genKeyPir();
        String publicKey = Base64Utils.encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64Utils.encodeToString(keyPair.getPrivate().getEncoded());
        JwtProperties result = new JwtProperties();
        if (duration != null) {
            result.setEffectiveTime(duration);
            result.setRefreshEffectiveTime(duration);
        }
        result.setIssuer("test");
        result.setAudience("test");
        result.setRsaPublicKey(publicKey);
        result.setRsaPrivateKey(privateKey);
        return result;
    }

    private static KeyPair genKeyPir() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
