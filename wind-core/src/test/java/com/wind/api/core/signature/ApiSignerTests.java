package com.wind.api.core.signature;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author wuxp
 * @date 2024-02-23 13:33
 **/
class ApiSignerTests {

    @Test
    void testSha256Sign() {
        String secretKey = "0241nl401kmdsai21o312..";
        ApiSignatureRequest signatureRequest = mockRequest();
        String sign = ApiSignAlgorithm.HMAC_SHA256.sign(signatureRequest, secretKey);
        Assertions.assertTrue(ApiSignAlgorithm.HMAC_SHA256.verify(copyAndReplaceSecretRequest(signatureRequest), secretKey, sign));
    }

    @Test
    void testSha256WithRsaSign() {
        KeyPair keyPair = genKeyPir();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        ApiSignatureRequest signatureRequest = mockRequest();
        String sign = ApiSignAlgorithm.SHA256_WITH_RSA.sign(signatureRequest, privateKey);
        Assertions.assertTrue(ApiSignAlgorithm.SHA256_WITH_RSA.verify(copyAndReplaceSecretRequest(signatureRequest), publicKey, sign));
    }

    private static ApiSignatureRequest copyAndReplaceSecretRequest(ApiSignatureRequest signatureRequest) {
        return ApiSignatureRequest.builder()
                .method(signatureRequest.getMethod())
                .requestPath(signatureRequest.getRequestPath())
                .timestamp(signatureRequest.getTimestamp())
                .nonce(signatureRequest.getNonce())
                .requestBody(signatureRequest.getRequestBody())
                .build();
    }

    private static ApiSignatureRequest mockRequest() {
        return ApiSignatureRequest.builder()
                .method("POST")
                .requestPath("/ap/v1/users")
                .timestamp("17182381131")
                .nonce("j12j34124i1j5219902103120")
                .requestBody("{id:\"1\"}")
                .build();
    }

    private KeyPair genKeyPir() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
