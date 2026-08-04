package com.wind.configcenter.core;

import com.google.common.collect.ImmutableMap;
import com.wind.common.WindConstants;
import com.wind.jackson.WindJson;
import com.wind.security.crypto.symmetric.AesTextEncryptor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wuxp
 * @date 2025-03-11 10:41
 **/
class ConfigFunctionEvaluatorTests {

    private final TextEncryptor textEncryptor = new AesTextEncryptor(RandomStringUtils.secure().nextAlphabetic(32), WindConstants.DEFAULT_TEXT);

    private final ConfigFunctionEvaluator evaluator = new ConfigFunctionEvaluator(new ConfigFunctionRootObject(textEncryptor, key -> {
        if (key.contains("text")) {
            return key.concat("text");
        }
        Map<String, String> config = ImmutableMap.of("userName", "a", "password", "abc",
                "name", "zhans", "accountPassword", "abc2", "rsaPublic", "1");
        return WindJson.toJsonString(config);
    }));

    @Test
    void testEvalNoFunctions() {
        String content = "example.name=zhangs@example.password=123456";
        String result = evaluator.eval("test", content);
        Assertions.assertEquals(content, result);
    }

    @Test
    void testEval() {
        String encrypt = textEncryptor.encrypt("test");
        // @{CRE('nobe_polardb_mysql','AccountName')}
        // @{CRE('nobe_polardb_mysql','AccountName')}
        String result = evaluator.eval("test", "example.name=@{DEC('" + encrypt + "')}&example.password=@{CRE('password','password')}");
        Assertions.assertEquals("example.name=test&example.password=abc", result);
        String result1 = evaluator.eval("test", "singleServerConfig:\n" +
                "  username: @{CRE('example','userName')}\n" +
                "  password: @{CRE('example','password')}\n" +
                "  address: redis://xxxx\n" +
                "  database: 11\n" +
                "threads: 0\n" +
                "nettyThreads: 0\n" +
                "codec: !<org.redisson.codec.Kryo5Codec> {}\n" +
                "transportMode: \"NIO\"");
        Assertions.assertEquals("singleServerConfig:\n" +
                "  username: a\n" +
                "  password: abc\n" +
                "  address: redis://xxxx\n" +
                "  database: 11\n" +
                "threads: 0\n" +
                "nettyThreads: 0\n" +
                "codec: !<org.redisson.codec.Kryo5Codec> {}\n" +
                "transportMode: \"NIO\"", result1);
    }

    @Test
    void testEvalWithSource() {
        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("test.example", "a");
        keyValues.put("test.example.enc", String.format("@{DEC('%s')}", textEncryptor.encrypt("test")));
        keyValues.put("test.user.name", "@{CRE('example','userName')}");
        keyValues.put("test.user.password", "@{CRE('example','password')}");
        keyValues.put("test.rds.accountName", "@{CRE('rds_dev','name')}");
        keyValues.put("test.rds.password", "@{CRE('rds_dev','accountPassword')}");
        keyValues.put("test.rds.value", "@{CRE('rds_dev','value')}");
        keyValues.put("test.jwt.key", "@{CRE('jwt_text_key')}");
        keyValues.put("test.jwt.rasPublicKey", "@{CRE('jwt_rsa_key','rsaPublic')}");
        keyValues.put("test.jwt.rasPrivateKey", "@{CRE('jwt_rsa_key','rasPrivate')}");
        MapPropertySource source = new MapPropertySource("test", keyValues);
        PropertySource<?> result = evaluator.eval(source);
        Assertions.assertEquals("abc2", result.getProperty("test.rds.password"));
        Assertions.assertEquals("a", result.getProperty("test.user.name"));
        Assertions.assertEquals("zhans", result.getProperty("test.rds.accountName"));
        Assertions.assertNull(result.getProperty("jwt_rsa_key.rasPrivate"));
    }
}
