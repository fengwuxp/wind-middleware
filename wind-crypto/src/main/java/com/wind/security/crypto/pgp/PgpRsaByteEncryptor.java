package com.wind.security.crypto.pgp;

import com.wind.common.WindConstants;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

import static com.wind.security.crypto.pgp.PgpKeyUtils.BOUNCY_CASTLE_NAME;

/**
 * PGP RSA 密钥字节加密器，使用 AES-256 加密
 *
 * @author wuxp
 * @date 2026-02-10 13:26
 **/
public class PgpRsaByteEncryptor implements BytesEncryptor {

    private final PGPPublicKey encryptionKey;

    private final PGPPrivateKey decryptionKey;

    public PgpRsaByteEncryptor(PGPPublicKey encryptionKey, PGPPrivateKey decryptionKey) {
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey;
    }

    @Override
    public byte[] encrypt(byte[] bytes) {
        ByteArrayOutputStream resultOutput = new ByteArrayOutputStream();
        try (ArmoredOutputStream armoredOutput = new ArmoredOutputStream(resultOutput)) {
            // 创建加密数据生成器
            PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                    new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
                            .setWithIntegrityPacket(true)
                            .setSecureRandom(new SecureRandom())
                            .setProvider(BOUNCY_CASTLE_NAME)
            );

            // 添加公钥加密方法
            encryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey).setProvider(BOUNCY_CASTLE_NAME));

            // 创建压缩数据生成器
            PGPCompressedDataGenerator compressedDataGenerator =
                    new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);

            try (OutputStream encryptedOut = encryptedDataGenerator.open(armoredOutput, new byte[4096]);
                 OutputStream compressedOut = compressedDataGenerator.open(encryptedOut);
                 OutputStream literalOut = new PGPLiteralDataGenerator().open(
                         // 文件名，可以为空
                         compressedOut, PGPLiteralData.UTF8, WindConstants.EMPTY, new Date(), new byte[4096]
                 )) {
                // 写入要加密的数据
                literalOut.write(bytes);

            }
            // 关闭流（必须按顺序关闭）
            compressedDataGenerator.close();
            encryptedDataGenerator.close();
        } catch (IOException | PGPException e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "PGP encryption failed", e);
        }
        return resultOutput.toByteArray();
    }

    @Override
    public byte[] decrypt(byte[] encryptedBytes) {
        try (InputStream in = new ByteArrayInputStream(encryptedBytes);
             InputStream decoderStream = PGPUtil.getDecoderStream(in)) {
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(decoderStream, new JcaKeyFingerprintCalculator());
            Object object = pgpObjectFactory.nextObject();

            // 如果第一个对象是 PGPEncryptedDataList
            PGPEncryptedDataList encryptedData = null;
            if (object instanceof PGPEncryptedDataList data) {
                encryptedData = data;
            } else {
                // 也可能是 PGPCompressedData 或 PGPOnePassSignatureList
                // 继续读取直到找到加密数据
                while ((object = pgpObjectFactory.nextObject()) != null) {
                    if (object instanceof PGPEncryptedDataList data) {
                        encryptedData = data;
                        break;
                    }
                }
                if (encryptedData == null) {
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "No encrypted data found in the input");
                }
            }

            // 查找可以解密的加密数据包
            PGPPublicKeyEncryptedData encryptedDataPacket = null;
            for (PGPEncryptedData encryptedDataItem : encryptedData) {
                if ((encryptedDataItem instanceof PGPPublicKeyEncryptedData pubKeyData) &&
                        decryptionKey != null && decryptionKey.getKeyID() == pubKeyData.getKeyIdentifier().getKeyId()) {
                    encryptedDataPacket = pubKeyData;
                    break;
                }
            }

            if (encryptedDataPacket == null) {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "Cannot find encrypted data that can be decrypted with the provided private key");
            }

            // 解密数据
            InputStream clearStream = encryptedDataPacket.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(BOUNCY_CASTLE_NAME).build(decryptionKey));
            // 处理解密后的数据
            PGPObjectFactory clearObjectFactory = new PGPObjectFactory(clearStream, new JcaKeyFingerprintCalculator());
            Object clearObject = clearObjectFactory.nextObject();

            // 处理压缩数据（如果存在）
            if (clearObject instanceof PGPCompressedData compressedData) {
                clearObjectFactory = new PGPObjectFactory(
                        compressedData.getDataStream(),
                        new JcaKeyFingerprintCalculator()
                );
                clearObject = clearObjectFactory.nextObject();
            }

            // 处理字面数据
            if (clearObject instanceof PGPLiteralData literalData) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream dataIn = literalData.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }

                // 验证完整性
                if (encryptedDataPacket.isIntegrityProtected() && !encryptedDataPacket.verify()) {
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "Message integrity check failed");
                }
                return out.toByteArray();
            } else {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR,
                        "Expected literal data packet, found: " + (clearObject != null ? clearObject.getClass().getName() : "null")
                );
            }
        } catch (IOException | PGPException e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "PGP decryption failed", e);
        }
    }


    /**
     * 创建 PGP RSA 加密器
     *
     * @param publicKey 公钥文本内容
     * @return TextEncryptor
     */
    public static TextEncryptor ofEncryptor(String publicKey) {
        PGPPublicKey pgpPublicKey = PgpKeyUtils.readPublicKey(publicKey);
        PgpRsaByteEncryptor delegate = new PgpRsaByteEncryptor(pgpPublicKey, null);
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                byte[] result = delegate.encrypt(text.getBytes(StandardCharsets.UTF_8));
                return new String(result, StandardCharsets.UTF_8);
            }

            @Override
            public String decrypt(String encryptedText) {
                throw new UnsupportedOperationException("Unsupported decrypt");
            }
        };
    }

    /**
     * 创建 PGP RSA 加密器
     *
     * @param privateKey 私钥文本内容
     * @param passphrase 私钥密码
     * @return PGP TextEncryptor
     */
    public static TextEncryptor ofDecryptor(String privateKey, String passphrase) {
        PGPPrivateKey pgpPrivateKey = PgpKeyUtils.readPrivateKey(privateKey, passphrase);
        PgpRsaByteEncryptor delegate = new PgpRsaByteEncryptor(null, pgpPrivateKey);
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                throw new UnsupportedOperationException("Unsupported encrypt");
            }

            @Override
            public String decrypt(String encryptedText) {
                byte[] result = delegate.decrypt(encryptedText.getBytes(StandardCharsets.UTF_8));
                return new String(result, StandardCharsets.UTF_8);
            }
        };
    }

    /**
     * 创建PGP RSA 加密器
     *
     * @param publicKey  公钥文本内容
     * @param privateKey 私钥文本内容
     * @param passphrase 私钥密码
     * @return PGP TextEncryptor
     */
    public static TextEncryptor text(String publicKey, String privateKey, String passphrase) {
        PGPPublicKey pgpPublicKey = PgpKeyUtils.readPublicKey(publicKey);
        PGPPrivateKey pgpPrivateKey = PgpKeyUtils.readPrivateKey(privateKey, passphrase);
        PgpRsaByteEncryptor delegate = new PgpRsaByteEncryptor(pgpPublicKey, pgpPrivateKey);
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                byte[] result = delegate.encrypt(text.getBytes(StandardCharsets.UTF_8));
                return new String(result, StandardCharsets.UTF_8);
            }

            @Override
            public String decrypt(String encryptedText) {
                byte[] result = delegate.decrypt(encryptedText.getBytes(StandardCharsets.UTF_8));
                return new String(result, StandardCharsets.UTF_8);
            }
        };
    }
}
