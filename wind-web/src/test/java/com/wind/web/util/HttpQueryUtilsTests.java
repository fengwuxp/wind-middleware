package com.wind.web.util;

import com.wind.web.servlet.util.HttpQueryUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author wuxp
 * @date 2024-02-21 16:05
 **/
class HttpQueryUtilsTests {

    @Test
    void testParseQueryParams() {
        Assertions.assertTrue(HttpQueryUtils.parseQueryParams(null).isEmpty());
        Map<String, String[]> queryParams = HttpQueryUtils.parseQueryParamsAsMap("name=张三&age=23&tags=t1&tags=t2");
        Assertions.assertNotNull(queryParams);
        Assertions.assertEquals("张三", queryParams.get("name")[0]);
    }

    @Test
    void testParseQueryParamsByEncoding() {
        Map<String, String[]> queryParams = HttpQueryUtils.parseQueryParamsAsMap("current=1&pageSize=10&nickname=%E6%B5%8B%E8%AF%95&orderFields" +
                "=GMT_MODIFIED&orderTypes=DESC&loadRoles=true");
        Assertions.assertNotNull(queryParams);
        Assertions.assertEquals("测试", queryParams.get("nickname")[0]);
    }

    @Test
    void testParseQueryParamsByEncodingRfc2396() {
        // https://juejin.cn/post/6844904034453864462#heading-2
        // https://www.ietf.org/rfc/rfc2396.txt
        String queryString = UriUtils.encodeQuery("rfc2396=*.,?-=+ (-12", StandardCharsets.UTF_8);
        Assertions.assertEquals("rfc2396=*.,?-=+%20(-12", queryString);
        Map<String, String[]> queryParams = HttpQueryUtils.parseQueryParamsAsMap(queryString);
        Assertions.assertNotNull(queryParams);
        Assertions.assertEquals("*.,?-=+ (-12", queryParams.get("rfc2396")[0]);
        queryString = UriUtils.decode("nickname=%E6%B5%8B%E8%AF%95%20%2B", StandardCharsets.UTF_8);
        Assertions.assertEquals("nickname=测试 +", queryString);
    }
}
