package com.wind.security.crypto;

import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 加密包装类
 *
 * @author wuxp
 * @date 2026-02-09 15:16
 **/
@AllArgsConstructor
public class Base64TextEncryptorWrapper implements TextEncryptor {

    private final BytesEncryptor delegate;

    @Override
    public String encrypt(@NonNull String text) {
        return Base64.getEncoder().encodeToString(delegate.encrypt(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String decrypt(@NonNull String encryptedText) {
        return new String(delegate.decrypt(Base64.getDecoder().decode(encryptedText)), StandardCharsets.UTF_8);
    }

    @NonNull
    public static TextEncryptor wrap(BytesEncryptor delegate) {
        return new Base64TextEncryptorWrapper(delegate);
    }
}
