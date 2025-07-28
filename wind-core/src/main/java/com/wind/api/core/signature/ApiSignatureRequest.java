package com.wind.api.core.signature;

import com.wind.common.WindConstants;
import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * API 签名加签对象
 *
 * @author wuxp
 * @date 2023-10-18 22:08
 * @see <a href="https://juejin.cn/post/6844904034453864462#heading-2">http请求中加号被替换为空格？源码背后的秘密</a>
 */
@Data
@Getter
@Builder
@FieldNameConstants
public class ApiSignatureRequest {

    /**
     * 需要 requestBody 参与签名的 Content-Type
     */
    private static final List<String> SIGNE_CONTENT_TYPES = Arrays.asList("application/json", "application/x-www-form-urlencoded");

    private static final String MD5_TAG = "Md5";

    /**
     * http 请求方法
     */
    private final String method;

    /**
     * http 请求 path，不包含查询参数和域名
     */
    private final String requestPath;

    /**
     * 32 位字符串
     */
    private final String nonce;

    /**
     * 时间戳
     */
    private final String timestamp;

    /**
     * 请求查询字符串
     */
    private final String queryString;

    /**
     * 请求体
     */
    private final String requestBody;

    private ApiSignatureRequest(String method, String requestPath, String nonce, String timestamp, String queryString, String requestBody) {
        AssertUtils.hasText(method, "method must not empty");
        AssertUtils.notNull(requestPath, "requestPath must not null");
        AssertUtils.hasText(nonce, "nonce must not empty");
        AssertUtils.hasText(timestamp, "timestamp must not empty");
        this.method = method.toUpperCase();
        this.requestPath = requestPath;
        this.nonce = nonce;
        this.timestamp = timestamp;
        // 将查询字符串 key 按照字典序排序
        this.queryString = buildCanonicalizedQueryString(parseQueryParamsAsMap(queryString));
        this.requestBody = requestBody;
    }

    /**
     * @return 获取摘要签名字符串
     */
    public String getSignTextForDigest() {
        StringBuilder result = new StringBuilder()
                .append(Fields.method).append(WindConstants.EQ).append(method).append(WindConstants.AND)
                .append(Fields.requestPath).append(WindConstants.EQ).append(requestPath).append(WindConstants.AND)
                .append(Fields.nonce).append(WindConstants.EQ).append(nonce).append(WindConstants.AND)
                .append(Fields.timestamp).append(WindConstants.EQ).append(timestamp);
        if (StringUtils.hasLength(queryString)) {
            result.append(WindConstants.AND)
                    .append(String.format("%s%s", Fields.queryString, MD5_TAG))
                    .append(WindConstants.EQ)
                    .append(DigestUtils.md5DigestAsHex(queryString.getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasLength(requestBody)) {
            result.append(WindConstants.AND)
                    .append(String.format("%s%s", Fields.requestBody, MD5_TAG))
                    .append(WindConstants.EQ)
                    .append(DigestUtils.md5DigestAsHex(requestBody.getBytes(StandardCharsets.UTF_8)));
        }
        return result.toString();
    }

    /**
     * @return 获取 Sha256WithRsa 签名字符串
     */
    public String getSignTextForSha256WithRsa() {
        return method + WindConstants.SPACE + requestPath + WindConstants.LF +
                timestamp + WindConstants.LF +
                nonce + WindConstants.LF +
                (StringUtils.hasText(queryString) ? queryString : WindConstants.EMPTY) + WindConstants.LF +
                (StringUtils.hasText(requestBody) ? requestBody : WindConstants.EMPTY) + WindConstants.LF;
    }

    /**
     * @return 获取按照字典序的查询字符串
     */
    @Nullable
    @VisibleForTesting
    static String buildCanonicalizedQueryString(Map<String, List<String>> queryParams) {
        if (ObjectUtils.isEmpty(queryParams)) {
            return null;
        }
        Map<String, List<String>> sortedKeyParams = new TreeMap<>(queryParams);
        return sortedKeyParams.entrySet()
                .stream()
                .map(entry -> {
                    if (ObjectUtils.isEmpty(entry.getValue())) {
                        return entry.getKey() + WindConstants.EQ;
                    }
                    return entry.getValue()
                            .stream()
                            .map(val -> String.format("%s=%s", entry.getKey(), val))
                            .collect(Collectors.joining(WindConstants.AND));
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(WindConstants.AND));
    }

    @VisibleForTesting
    static Map<String, List<String>> parseQueryParamsAsMap(String queryString) {
        Map<String, List<String>> result = new HashMap<>();
        if (StringUtils.hasText(queryString)) {
            String[] parts = decodeQueryString(queryString).split(WindConstants.AND);
            for (String part : parts) {
                String[] keyValues = part.split(WindConstants.EQ, 2);
                String key = keyValues[0];
                List<String> values = result.get(key);
                if (values == null) {
                    values = new ArrayList<>();
                }
                if (keyValues.length == 2) {
                    values.add(keyValues[1]);
                }
                result.put(key, values);
            }
        }
        return result;
    }

    @NotNull
    @VisibleForTesting
    static String decodeQueryString(String queryString) {
        return URLDecoder.decode(queryString, StandardCharsets.UTF_8);
    }

    public static boolean signRequireRequestBody(@Nullable String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return false;
        }
        return SIGNE_CONTENT_TYPES.stream().anyMatch(contentType::startsWith);
    }
}
