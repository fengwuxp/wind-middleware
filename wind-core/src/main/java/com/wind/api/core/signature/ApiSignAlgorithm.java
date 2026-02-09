package com.wind.api.core.signature;

import com.wind.signature.SignatureAlgorithm;
import com.wind.signature.WindTextSigner;
import com.wind.signature.algorithm.HmacShaByteSigner;
import com.wind.signature.algorithm.ShaWithRsaByteSigner;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuxp
 * @date 2024-03-05 09:57
 **/
@Getter
@AllArgsConstructor
public enum ApiSignAlgorithm implements ApiSigner {

    /**
     * 摘要签名
     * 参见：https://www.yuque.com/suiyuerufeng-akjad/wind/qal4b72cxw84cu6g
     */
    HMAC_SHA256(SignatureAlgorithm.HMAC_SHA256),

    HMAC_SHA512(SignatureAlgorithm.HMAC_SHA512),

    /**
     * 参见：https://www.yuque.com/suiyuerufeng-akjad/wind/qal4b72cxw84cu6g
     */
    SHA256_WITH_RSA(SignatureAlgorithm.SHA256_WITH_RSA),
    ;

    /**
     * 签名算法
     */
    private final SignatureAlgorithm algorithm;

    /**
     * 签名实现
     */
    private final ApiSigner signer;

    ApiSignAlgorithm(SignatureAlgorithm algorithm) {
        this(algorithm, factory(algorithm));
    }

    @Override
    public String sign(ApiSignatureRequest request, String secretKey) {
        return signer.sign(request, secretKey);
    }

    @Override
    public boolean verify(ApiSignatureRequest request, String secretKey, String sign) {
        return signer.verify(request, secretKey, sign);
    }

    /**
     * 创建签名器
     *
     * @param algorithm 签名算法
     * @return 签名器
     */
    private static ApiSigner factory(SignatureAlgorithm algorithm) {
        return switch (algorithm) {
            case HMAC_SHA256 -> new ApiSignerWrapper(HmacShaByteSigner.hmacSha256Base64(), algorithm);
            case HMAC_SHA512 -> new ApiSignerWrapper(HmacShaByteSigner.hmacSha512Base64(), algorithm);
            case SHA256_WITH_RSA -> new ApiSignerWrapper(ShaWithRsaByteSigner.sha256WithRsaBas4(), algorithm);
        };
    }

    /**
     * Api 签名实现
     *
     * @author wuxp
     * @date 2026-02-09 17:05
     **/
    private record ApiSignerWrapper(WindTextSigner delegate, SignatureAlgorithm algorithm) implements ApiSigner {

        @Override
        public String sign(ApiSignatureRequest request, String secretKey) {
            return delegate.sign(request.getSignText(algorithm), secretKey);
        }

        @Override
        public boolean verify(ApiSignatureRequest request, String secretKey, String sign) {
            return delegate.verify(request.getSignText(algorithm), secretKey, sign);
        }
    }

}
