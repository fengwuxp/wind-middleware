package com.wind.security.crypto.symmetric;


import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 对称加密：AES
 * AES（Advanced Encryption Standard，高级加密标准）是一种对称加密算法，即同一个密钥用于加密和解密。
 * AES被广泛用于保护数据的安全性。AES支持128位、192位和256位密钥长度，常见的块大小是128位。
 *
 * @author wuxp
 * @date 2024-07-15 13:02
 **/
public class AesTextEncryptor implements TextEncryptor {

    private final AesBytesEncryptor bytesEncryptor;

    public AesTextEncryptor(String password, String salt) {
        this(password, salt, null);
    }

    /**
     * 初始化向量
     * IV（Initialization Vector，初始化向量）主要作用是增加加密的安全性，避免相同的明文在相同的密钥下得到相同的密文。
     * 随机化初始块：IV使得即使相同的明文在相同的密钥下加密，也会产生不同的密文。它为加密过程提供了随机性。
     * 防止模式攻击：使用IV可以防止某些类型的攻击（如词典攻击和重放攻击），提高加密的安全性。
     * 保持解密正确性：解密过程中需要使用相同的IV，确保解密得到正确的明文。
     * {@link AesBytesEncryptor#AesBytesEncryptor(String, CharSequence, BytesKeyGenerator, AesBytesEncryptor.CipherAlgorithm)
     * AesBytesEncryptor(String, CharSequence, BytesKeyGenerator, CipherAlgorithm)}.
     *
     * @param password    the password value
     * @param salt        the hex-encoded salt value
     * @param ivGenerator the generator used to generate the initialization vector
     */
    public AesTextEncryptor(String password, String salt, BytesKeyGenerator ivGenerator) {
        this.bytesEncryptor = new AesBytesEncryptor(password, new String(Hex.encode(salt.getBytes(StandardCharsets.UTF_8))), ivGenerator);
    }

    /**
     * AES 加密
     *
     * @param text 待加密的文本
     * @return base64 编码加密结果
     */
    @Override
    public String encrypt(String text) {
        return Base64.getEncoder().encodeToString(bytesEncryptor.encrypt(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(String encryptedText) {
        byte[] bytes = Base64.getDecoder().decode(encryptedText);
        return new String(bytesEncryptor.decrypt(bytes), StandardCharsets.UTF_8);
    }
}
