package com.wind.api.core.signature;

import jakarta.validation.constraints.NotNull;

/**
 * Api Signer
 *
 * @author wuxp
 * @date 2024-02-23 12:48
 **/
public interface ApiSigner {


    /**
     * 生成签名
     *
     * @param request   签名请求
     * @param secretKey 签名秘钥
     * @return 签名结果
     */
    @NotNull
    String sign(@NotNull ApiSignatureRequest request, String secretKey);

    /**
     * 签名验证
     *
     * @param request   用于验证签名的请求
     * @param secretKey 签名秘钥
     * @param sign      待验证的签名
     * @return 签名验证是否通过
     */
    boolean verify(@NotNull ApiSignatureRequest request, String secretKey, @NotNull String sign);
}
