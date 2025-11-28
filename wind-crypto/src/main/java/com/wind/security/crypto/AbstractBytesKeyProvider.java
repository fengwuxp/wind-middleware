package com.wind.security.crypto;

import org.springframework.security.crypto.keygen.BytesKeyGenerator;

import java.util.function.Function;

/**
 * @author wuxp
 * @date 2024-07-13 12:46
 **/
public abstract class AbstractBytesKeyProvider<T> implements BytesKeyGenerator {

    private static final Function<String, byte[]> NONE = String::getBytes;

    /**
     * 秘钥字节数组
     */
    private final byte[] keyBytes;

    /**
     * 用于解密秘钥的 Decryptor
     */
    private final Function<String, byte[]> keyDecryptor;

    /**
     * 用于加载秘钥的参数
     */
    private final T loadKeyParam;

    protected AbstractBytesKeyProvider(T loadKeyParam, Function<String, byte[]> keyDecryptor) {
        this.loadKeyParam = loadKeyParam;
        this.keyDecryptor = keyDecryptor;
        this.keyBytes = loadKeyBytes();
    }

    protected AbstractBytesKeyProvider(T loadKeyParam) {
        this(loadKeyParam, NONE);
    }

    @Override
    public int getKeyLength() {
        return keyBytes.length;
    }

    @Override
    public byte[] generateKey() {
        return keyBytes;
    }

    protected abstract String loadKey(T loadKeyParam);

    private byte[] loadKeyBytes() {
        String key = loadKey(this.loadKeyParam);
        return keyDecryptor == null ? key.getBytes() : keyDecryptor.apply(key);
    }
}
