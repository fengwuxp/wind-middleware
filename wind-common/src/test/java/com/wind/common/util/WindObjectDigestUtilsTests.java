package com.wind.common.util;

import com.google.common.collect.ImmutableMap;
import com.wind.common.WindConstants;
import com.wind.common.WindDateFormatPatterns;
import com.wind.common.WindDateFormater;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wuxp
 * @date 2024-08-05 16:41
 **/
class WindObjectDigestUtilsTests {

    @Test
    void testGenSha256WithText() {
        String sha = WindObjectDigestUtils.sha256("123456");
        Assertions.assertEquals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", sha);
    }

    @Test
    void testGenSha256WithPrimitive() {
        Assertions.assertEquals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", WindObjectDigestUtils.sha256(123456));
        Assertions.assertEquals("b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b", WindObjectDigestUtils.sha256(true));
        Assertions.assertEquals("633226122f0adb51fb56c68a9dbdba0ed3f2c4359251755cf44b6da46e0152e6", WindObjectDigestUtils.sha256(new Integer[]{1, 3, 4}));
    }

    @Test
    void testGenSha256WithCollection() {
        Assertions.assertEquals("3eadb3a193c717651741f576ec65cfe8d9829bee8898157e18c85cd18482c4dd", WindObjectDigestUtils.sha256(Arrays.asList(1, 2, 3)));
        Assertions.assertEquals("a6124dcc2746aacd8b55b8adaccfe33620b4000897607c47f6665cc424b3baf8", WindObjectDigestUtils.sha256(Map.of("a", 1, "b", 2)));
        HashMap<Object, Object> target = new HashMap<>();
        target.put("demo", "");
        target.put("k", null);
        Assertions.assertEquals("5f54ab3d2a4caca511cb1f9a25232591d3409f2e57ba489c8b46ef733bed5c07", WindObjectDigestUtils.sha256(target));
    }


    @Test
    void testGenSha256Text() throws Exception {
        WindObjectDigestWindObjectDigestExample target = mockExample();
        String text = WindObjectDigestUtils.genSha256TextWithObject(target, WindReflectUtils.getFieldNames(target.getClass()), null, WindConstants.LF);
        Assertions.assertEquals("age=26\n" +
                "birthday=1581379200000\n" +
                "demo={e3=1.30912&id=demo&k1=k&l2=false&name=}\n" +
                "fees=[0, 23, 99]\n" +
                "id=1\n" +
                "myDate=1581350400000\n" +
                "myTags={name=a&value=a1},{name=b&value=b1}\n" +
                "name=\n" +
                "names=a,b,c\n" +
                "sex=M\n" +
                "tags={a1=test&c1=1&zh={name=001&value=exampleTag}}\n" +
                "timeOfBirth=1581379975000\n" +
                "yes=false", text);
    }

    @Test
    void testGenSha25TextWithNames() throws Exception {
        WindObjectDigestWindObjectDigestExample target = mockExample();
        String text = WindObjectDigestUtils.genSha256TextWithObject(target, Arrays.asList("name", "id", "sex", "myTags", "yes"), null, WindConstants.LF);
        Assertions.assertEquals("id=1\n" +
                "myTags={name=a&value=a1},{name=b&value=b1}\n" +
                "name=\n" +
                "sex=M\n" +
                "yes=false", text);
    }

    @Test
    void testSha256() throws Exception {
        WindObjectDigestWindObjectDigestExample target = mockExample();
        String expected = WindObjectDigestUtils.sha256(target);
        Assertions.assertEquals(expected, WindObjectDigestUtils.sha256(target));
        target.setId(RandomUtils.nextLong());
        Assertions.assertNotEquals(expected, WindObjectDigestUtils.sha256(target));
    }

    @Test
    void testSha256WithPrefix() throws Exception {
        WindObjectDigestWindObjectDigestExample target = mockExample();
        String expected = WindObjectDigestUtils.sha256(target, "Example");
        Assertions.assertEquals(expected, WindObjectDigestUtils.sha256(target, "Example"));
        Assertions.assertNotEquals(expected, WindObjectDigestUtils.sha256(target, "E1"));
        target.setId(RandomUtils.nextLong());
        Assertions.assertNotEquals(expected, WindObjectDigestUtils.sha256(target, "Example"));
    }

    @Test
    void testSha256WithNames() throws Exception {
        List<String> names = Arrays.asList("name", "id", "sex", "myTags", "yes");
        WindObjectDigestWindObjectDigestExample target = mockExample();
        String expected = WindObjectDigestUtils.sha256WithNames(target, names);
        Assertions.assertEquals(expected, WindObjectDigestUtils.sha256WithNames(target, names));
        target.setFees(new int[]{1});
        Assertions.assertEquals(expected, WindObjectDigestUtils.sha256WithNames(target, names));
        target.setId(RandomUtils.nextLong());
        Assertions.assertNotEquals(expected, WindObjectDigestUtils.sha256WithNames(target, names));
    }

    private WindObjectDigestWindObjectDigestExample mockExample() throws Exception {
        WindObjectDigestWindObjectDigestExample result = new WindObjectDigestWindObjectDigestExample();
        result.setId(1L);
        result.setTags(ImmutableMap.of("c1", "1", "a1", "test", "zh", new WindObjectDigestTag("001", "exampleTag")));
        result.setSex(WindObjectDigestSex.M);
        result.setAge(26);
        LocalDateTime birthday = LocalDateTime.parse("2020-02-11 00:12:55", WindDateFormater.YYYY_MM_DD_HH_MM_SS.getFormatter());
        result.setTimeOfBirth(birthday);
        result.setBirthday(LocalDate.parse("2020-02-11", WindDateFormater.YYYY_MM_DD.getFormatter()));
        result.setMyDate(DateUtils.parseDate("2020-02-11", WindDateFormatPatterns.YYYY_MM_DD));
        result.setMyTags(Arrays.asList(new WindObjectDigestTag("a", "a1"), new WindObjectDigestTag("b", "b1")));
        result.setNames(new String[]{"a", "b", "c"});
        result.setFees(new int[]{0, 23, 99});
        WindObjectDigestDemo windObjectDigestDemo = new WindObjectDigestDemo();
        windObjectDigestDemo.setId("demo");
        windObjectDigestDemo.setK1("k");
        windObjectDigestDemo.setL2(false);
        windObjectDigestDemo.setE3(1.30912d);
        result.setDemo(windObjectDigestDemo);
        return result;
    }

    @Data
    public static class WindObjectDigestExampleSupper {

        private Long id;

        private Map<String, Object> tags;
    }

    public enum WindObjectDigestSex {
        N,
        M
    }

    @AllArgsConstructor
    public static class WindObjectDigestTag {

        private String name;

        private String value;
    }


    @Data
    public static class WindObjectDigestDemo {

        private String id;

        private String name;

        private String k1;

        private boolean l2;

        private double e3;
    }


    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    @Data
    public static class WindObjectDigestWindObjectDigestExample extends WindObjectDigestExampleSupper {

        private String name;

        private int age;

        private WindObjectDigestSex sex;

        private boolean yes;

        private String[] names;

        private int[] fees;

        private List<WindObjectDigestTag> myTags;

        private LocalDateTime timeOfBirth;

        private LocalDate birthday;

        private Date myDate;

        private WindObjectDigestDemo demo;
    }
}
