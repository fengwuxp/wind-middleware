package com.wind.signature;

import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * @author wuxp
 * @date 2026-02-09 17:15
 **/
public final class SimpleTextSignerWrapper {

    public SimpleTextSignerWrapper() {
        throw new AssertionError();
    }

    /**
     * 签名结果进行base64编码
     *
     * @param delegate 签名实现
     * @return 签名结果进行base64编码
     */
    public static WindTextSigner base64(WindByteSigner delegate) {
        return new WindTextSigner() {
            @Override
            public @NonNull String sign(@NonNull String signText, @NonNull String secretKey) {
                byte[] bytes = delegate.sign(signText.getBytes(), secretKey.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(bytes);
            }

            @Override
            public boolean verify(String signText, String secretKey, String base64Sign) {
                return delegate.verify(signText.getBytes(), secretKey.getBytes(StandardCharsets.UTF_8), Base64.getDecoder().decode(base64Sign));
            }
        };
    }

    /**
     * 签名结果进行16进制编码
     *
     * @param delegate 签名实现
     * @return 签名结果进行16进制编码
     */
    public static WindTextSigner hex(WindByteSigner delegate) {
        return new WindTextSigner() {
            @Override
            public @NonNull String sign(@NonNull String signText, @NonNull String secretKey) {
                byte[] bytes = delegate.sign(signText.getBytes(), secretKey.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(bytes);
            }

            @Override
            public boolean verify(String signText, String secretKey, String sign) {
                return delegate.verify(signText.getBytes(), secretKey.getBytes(StandardCharsets.UTF_8), HexFormat.of().parseHex(sign));
            }
        };
    }
}
