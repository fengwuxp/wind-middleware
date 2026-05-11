package com.wind.web.util;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wind.core.ProtocolValueFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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


    @Test
    void shouldReturnEmptyStringWhenQueryParamsIsNull() {
        String result = HttpQueryUtils.formatQueryString(
                null,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("", result);
    }

    @Test
    void shouldFormatMapParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "张三");
        params.put("age", 18);
        params.put("empty", null);

        String result = HttpQueryUtils.formatQueryString(
                params,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("name=%E5%BC%A0%E4%B8%89&age=18", result);
    }

    @Test
    void shouldFormatSimpleBean() {
        SimpleQuery query = new SimpleQuery();
        query.setName("tom");
        query.setAge(20);

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("name=tom&age=20", result);
    }

    @Test
    void shouldUseJsonPropertyName() {
        JacksonPropertyQuery query = new JacksonPropertyQuery();
        query.setUserName("coder");

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("user_name=coder", result);
    }

    @Test
    void shouldUseFastJsonFieldName() {
        FastJsonQuery query = new FastJsonQuery();
        query.setUserName("coder");

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("user_name=coder", result);
    }

    @Test
    void shouldUseJsonAliasWhenNoJsonPropertyOrFastJsonPresent() {
        JacksonAliasQuery query = new JacksonAliasQuery();
        query.setUserName("coder");

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("user_name=coder", result);
    }

    @Test
    void shouldUseNonStandardValueSerializable() throws Throwable {
        SerializableValueQuery query = new SerializableValueQuery();
        query.setStatus(new CustomStatus("paid"));

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("status=PAID", result);
    }

    @Test
    void shouldSerializeCollectionWithRepeatMode() {
        CollectionQuery query = new CollectionQuery();
        query.setIds(Arrays.asList(1, 2, 3));

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("ids=1&ids=2&ids=3", result);
    }

    @Test
    void shouldSerializeCollectionWithBracketsMode() {
        CollectionQuery query = new CollectionQuery();
        query.setIds(Arrays.asList(1, 2, 3));

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.BRACKETS
        );

        assertEquals("ids%5B%5D=1&ids%5B%5D=2&ids%5B%5D=3", result);
    }

    @Test
    void shouldSerializeCollectionWithCommaSeparatedMode() {
        CollectionQuery query = new CollectionQuery();
        query.setIds(Arrays.asList(1, 2, 3));

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.COMMA_SEPARATED
        );

        assertEquals("ids=1%2C2%2C3", result);
    }

    @Test
    void shouldSerializeArrayValues() {
        ArrayQuery query = new ArrayQuery();
        query.setIds(new Long[]{1L, 2L});

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("ids=1&ids=2", result);
    }

    @Test
    void shouldIgnoreNullItemInCollection() {
        CollectionQuery query = new CollectionQuery();
        query.setIds(Arrays.asList(1, null, 3));

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("ids=1&ids=3", result);
    }

    @Test
    void shouldDefaultToRepeatModeWhenCollectionModeIsNull() {
        CollectionQuery query = new CollectionQuery();
        query.setIds(Arrays.asList(1, 2));

        String result = HttpQueryUtils.formatQueryString(query, null);

        assertEquals("ids=1&ids=2", result);
    }

    @Test
    void shouldSupportFieldOnlyProperty() {
        FieldOnlyQuery query = new FieldOnlyQuery("A001");

        String result = HttpQueryUtils.formatQueryString(
                query,
                HttpQueryUtils.CollectionParamMode.REPEAT
        );

        assertEquals("code=A001", result);
    }

    static class SimpleQuery {

        private String name;

        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    public static class JacksonPropertyQuery {

        private String userName;

        @JsonProperty("user_name")
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class FastJsonQuery {

        @JSONField(name = "user_name")
        private String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class JacksonAliasQuery {

        private String userName;

        @JsonAlias({"user_name", "username"})
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    public static class SerializableValueQuery {

        private CustomStatus status;

        public CustomStatus getStatus() {
            return status;
        }

        public void setStatus(CustomStatus status) {
            this.status = status;
        }
    }

    static class CollectionQuery {

        private List<Integer> ids;

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }

    static class ArrayQuery {

        private Long[] ids;

        public Long[] getIds() {
            return ids;
        }

        public void setIds(Long[] ids) {
            this.ids = ids;
        }
    }

    static class FieldOnlyQuery {

        private final String code;

        FieldOnlyQuery(String code) {
            this.code = code;
        }
    }

    static class CustomStatus implements ProtocolValueFormatter {

        private final String value;

        CustomStatus(String value) {
            this.value = value;
        }

        @Override
        public String format() {
            return value.toUpperCase();
        }
    }
}
