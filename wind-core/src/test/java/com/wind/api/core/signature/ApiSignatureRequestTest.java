package com.wind.api.core.signature;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ApiSignatureRequestTest {


    @Test
    void testTetSignTextSpecialSymbols() {
        // a=1&b=2&c=a&,cd
        ApiSignatureRequest request = buildRequest("a=1&b=2&c=a%2526,cd", "{}");
        Assertions.assertEquals("method=POST&requestPath=/api/v1/example/users&nonce=jlj3rn2930d-123210dq&timestamp=123456789&queryStringMd5" +
                "=4d168edee991968e65e879a3746aefc0&requestBodyMd5=99914b932bd37a50b983c5e7c90ae93b", request.getSignTextForDigest());
        Assertions.assertEquals("POST /api/v1/example/users\n" +
                "123456789\n" +
                "jlj3rn2930d-123210dq\n" +
                "a=1&b=2&c=a%26,cd\n" +
                "{}\n", request.getSignTextForSha256WithRsa());
    }

    @Test
    void testTetSignText() {
        ApiSignatureRequest request = buildRequest("a=1&b=2&c=b,cd", "{}");
        Assertions.assertEquals("method=POST&requestPath=/api/v1/example/users&nonce=jlj3rn2930d-123210dq&timestamp=123456789&queryStringMd5" +
                "=8a1e3c93105e6af6494526bd28a55a65&requestBodyMd5=99914b932bd37a50b983c5e7c90ae93b", request.getSignTextForDigest());
        Assertions.assertEquals("POST /api/v1/example/users\n" +
                "123456789\n" +
                "jlj3rn2930d-123210dq\n" +
                "a=1&b=2&c=b,cd\n" +
                "{}\n", request.getSignTextForSha256WithRsa());
    }

    @Test
    void testTetSignTextNoneQueryString() {
        ApiSignatureRequest request = buildRequest(null, "{}");
        Assertions.assertEquals("method=POST&requestPath=/api/v1/example/users&nonce=jlj3rn2930d-123210dq&timestamp=123456789&requestBodyMd5" +
                "=99914b932bd37a50b983c5e7c90ae93b", request.getSignTextForDigest());
        Assertions.assertEquals("POST /api/v1/example/users\n" +
                "123456789\n" +
                "jlj3rn2930d-123210dq\n" +
                "\n" +
                "{}\n", request.getSignTextForSha256WithRsa());
    }

    @Test
    void testTetSignTextNoneRequestBody() {
        ApiSignatureRequest request = buildRequest("a=1&b=2&c=b,cd", null);
        Assertions.assertEquals("method=GET&requestPath=/api/v1/example/users&nonce=jlj3rn2930d-123210dq&timestamp=123456789&queryStringMd5" +
                "=8a1e3c93105e6af6494526bd28a55a65", request.getSignTextForDigest());
        Assertions.assertEquals("GET /api/v1/example/users\n" +
                "123456789\n" +
                "jlj3rn2930d-123210dq\n" +
                "a=1&b=2&c=b,cd\n" +
                "\n", request.getSignTextForSha256WithRsa());
    }

    @Test
    void testCanonicalizedQueryString() {
        ApiSignatureRequest request = buildRequest("age=36&name=zhans&tags=tag0&tags=tag1&empty=", "{}");
        Assertions.assertEquals("age=36&empty=&name=zhans&tags=tag0&tags=tag1", request.getQueryString());
        Assertions.assertEquals("method=POST&requestPath=/api/v1/example/users&nonce=jlj3rn2930d-123210dq&timestamp=123456789&queryStringMd5" +
                "=c79c749e047d59f40baef9a139abe834&requestBodyMd5=99914b932bd37a50b983c5e7c90ae93b", request.getSignTextForDigest());
        Assertions.assertEquals("POST /api/v1/example/users\n" +
                "123456789\n" +
                "jlj3rn2930d-123210dq\n" +
                "age=36&empty=&name=zhans&tags=tag0&tags=tag1\n" +
                "{}\n", request.getSignTextForSha256WithRsa());
    }

    @Test
    void testSignRequireRequestBody() {
        Assertions.assertFalse(ApiSignatureRequest.signRequireRequestBody(null));
        Assertions.assertFalse(ApiSignatureRequest.signRequireRequestBody(""));
        Assertions.assertFalse(ApiSignatureRequest.signRequireRequestBody("text/html"));
        Assertions.assertFalse(ApiSignatureRequest.signRequireRequestBody("application/xml"));
        Assertions.assertTrue(ApiSignatureRequest.signRequireRequestBody("application/json"));
        Assertions.assertTrue(ApiSignatureRequest.signRequireRequestBody("application/json;chart=UTF-8"));
        Assertions.assertTrue(ApiSignatureRequest.signRequireRequestBody("application/x-www-form-urlencoded"));
        Assertions.assertTrue(ApiSignatureRequest.signRequireRequestBody("application/x-www-form-urlencoded;chart=UTF-8"));
    }

    @Test
    void testParseQueryParamsAsMap() {
        Map<String, List<String>> queryParams = ApiSignatureRequest.parseQueryParamsAsMap("a=1%2B2&b=1 2&h=1=1");
        Assertions.assertEquals("1+2", queryParams.get("a").getFirst());
        Assertions.assertEquals("1 2", queryParams.get("b").getFirst());
        Assertions.assertEquals("1=1", queryParams.get("h").getFirst());
    }

    @Test
    void testBuildCanonicalizedQueryString() {
        Map<String, List<String>> queryParams = ApiSignatureRequest.parseQueryParamsAsMap("a=1%2B2&b=1 2&h=1=1");
        String queryString = ApiSignatureRequest.buildCanonicalizedQueryString(queryParams);
        Assertions.assertEquals("a=1+2&b=1 2&h=1=1", queryString);
    }

    @Test
    void testDecodeQueryString() {
        String queryString = ApiSignatureRequest.decodeQueryString("a%3D1%2B2%26b%3D1%202%26h%3D1%3D1");
        Assertions.assertEquals("a=1+2&b=1 2&h=1=1", queryString);
    }

    private static ApiSignatureRequest buildRequest(String queryString, String requestBody) {
        return ApiSignatureRequest.builder()
                .method(requestBody == null ? "GET" : "POST")
                .requestPath("/api/v1/example/users")
                .queryString(queryString)
                .requestBody(requestBody)
                .timestamp("123456789")
                .nonce("jlj3rn2930d-123210dq")
                .build();
    }
}