package com.wind.security.crypto.pgp;


import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

/**
 * PGP 密钥工具类
 *
 * @author wuxp
 * @date 2026-02-09 14:22
 **/
@Slf4j
public final class PgpKeyUtils {

    static final String BOUNCY_CASTLE_NAME = "BC";

    static {
        if (Security.getProvider(BOUNCY_CASTLE_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private PgpKeyUtils() {
        throw new AssertionError();
    }

    /**
     * 读取 PGP 公钥
     *
     * @param publicKey 公钥内容
     * @return 公钥
     */
    @NonNull
    public static PGPPublicKey readPublicKey(@NonNull String publicKey) {
        return readPublicKey(new ByteArrayInputStream(publicKey.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 读取 PGP 公钥
     * 注意：读取完成后会自动关闭流
     *
     * @param input 输入流
     * @return 公钥
     */
    @NonNull
    public static PGPPublicKey readPublicKey(@NonNull InputStream input) {
        try (InputStream decoderStream = PGPUtil.getDecoderStream(input)) {
            PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                    decoderStream,
                    new JcaKeyFingerprintCalculator()
            );

            // 首先尝试找加密密钥
            PGPPublicKey encryptionKey = findEncryptionKey(pgpPub);
            if (encryptionKey != null) {
                return encryptionKey;
            }

            // 如果没有专门的加密密钥，使用第一个有效密钥
            PGPPublicKey firstValidKey = findFirstValidKey(pgpPub);
            if (firstValidKey != null) {
                return firstValidKey;
            }

        } catch (IOException | PGPException exception) {
            log.error("Failed to read PGP public key", exception);
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                    "PGP public key read failed", exception);
        }
        throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                "No valid PGP public key found");
    }

    private static PGPPublicKey findEncryptionKey(PGPPublicKeyRingCollection pgpPub) {
        for (PGPPublicKeyRing keyRing : pgpPub) {
            for (PGPPublicKey key : keyRing) {
                if (key.isEncryptionKey() && !key.hasRevocation() && isActive(key)) {
                    return key;
                }
            }
        }
        return null;
    }

    private static PGPPublicKey findFirstValidKey(PGPPublicKeyRingCollection pgpPub) {
        for (PGPPublicKeyRing keyRing : pgpPub) {
            for (PGPPublicKey key : keyRing) {
                if (!key.hasRevocation() && isActive(key)) {
                    return key;
                }
            }
        }
        return null;
    }

    private static boolean isActive(PGPPublicKey key) {
        if (key.getValidSeconds() == 0) {
            return true; // 永不过期
        }

        Date creationTime = key.getCreationTime();
        long expirationTime = creationTime.getTime() + (key.getValidSeconds() * 1000L);
        return System.currentTimeMillis() <= expirationTime;
    }

    @NonNull
    public static PGPPublicKey readPublicKeyWithFile(@NonNull String filePath) {
        try {
            return readPublicKey(new FileInputStream(filePath));
        } catch (FileNotFoundException exception) {
            log.error("PGP public key file not found: {}", filePath, exception);
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                    "PGP public key file not found: " + filePath, exception);
        }
    }

    private static PGPPrivateKey findFirstPrivateKey(PGPSecretKeyRingCollection pgpSec,
                                                     String passphrase)
            throws PGPException {
        for (PGPSecretKeyRing keyRing : pgpSec) {
            for (PGPSecretKey key : keyRing) {
                if (!key.isPrivateKeyEmpty()) {
                    try {
                        PGPPrivateKey privateKey = key.extractPrivateKey(
                                new JcePBESecretKeyDecryptorBuilder()
                                        .setProvider("BC")
                                        .build(passphrase.toCharArray())
                        );
                        if (privateKey != null) {
                            return privateKey;
                        }
                    } catch (PGPException e) {
                        log.debug("Failed to extract private key", e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 读取 PGP 私钥环
     *
     * @param input 输入流
     * @return 私钥环
     */
    public static PGPSecretKeyRing readSecretKeyRing(@NonNull InputStream input) {
        try (InputStream decoderStream = PGPUtil.getDecoderStream(input)) {
            PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                    decoderStream,
                    new JcaKeyFingerprintCalculator()
            );

            // 获取第一个私钥环（通常只有一个）
            Iterator<PGPSecretKeyRing> keyRings = pgpSec.getKeyRings();
            if (keyRings.hasNext()) {
                return keyRings.next();
            } else {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "No PGP secret key ring found in input");
            }
        } catch (IOException | PGPException exception) {
            log.error("Failed to read PGP secret key ring", exception);
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "PGP secret key ring read failed", exception);
        }
    }

    /**
     * 读取 PGP 私钥环
     *
     * @param privateKeyContent 私钥内容
     * @return 私钥环
     */
    public static PGPSecretKeyRing readSecretKeyRing(@NonNull String privateKeyContent) {
        return readSecretKeyRing(new ByteArrayInputStream(privateKeyContent.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 读取 PGP 私钥环
     *
     * @param filePath 私钥文件路径
     * @return 私钥环
     */
    @NonNull
    public static PGPSecretKeyRing readSecretKeyRingWithFile(@NonNull String filePath) {
        try {
            return readSecretKeyRing(new FileInputStream(filePath));
        } catch (FileNotFoundException exception) {
            log.error("PGP private ring file not found: {}", filePath, exception);
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "PGP private key file not found: " + filePath, exception);
        }
    }
}