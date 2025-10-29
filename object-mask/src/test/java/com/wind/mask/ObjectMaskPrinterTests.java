package com.wind.mask;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.ImmutableMap;
import com.wind.mask.annotation.Sensitive;
import com.wind.mask.masker.json.JsonStringMasker;
import com.wind.mask.masker.json.MapObjectMasker;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static com.wind.mask.ObjectMaskPrinter.IdentityLimitPrinter.MAX_COLLECTION_SIZE;

/**
 * @author wuxp
 * @date 2024-03-11 13:36
 **/
class ObjectMaskPrinterTests {

    private final ObjectMaskPrinter printer = createPrinter();

    private ObjectSanitizePrinterExample example;

    @BeforeEach
    void setup() {
        example = new ObjectSanitizePrinterExample();
        example.setName("zhangs");
        example.setAk("AK_test");
        example.setCode("1002");
        example.setDesc("测试");
        example.setZed(true);
        example.setAge(11);
        example.setDepthObject(new HashMap<>());
    }

    private ObjectMaskPrinter createPrinter() {
        List<MaskRuleGroup> ruleGroups = MaskRuleGroup.builder()
                .form(Map.class)
                .of(ObjectMasker.ASTERISK)
                .build();
        return new ObjectMaskPrinter(new MaskRuleRegistry(ruleGroups));
    }

    @Test
    void testSanitizePrimitiveArray() {
        Assertions.assertEquals("[1, 2, 3]", printer.mask(new byte[]{1, 2, 3}));
        Assertions.assertEquals("[[1, 2, 3], [2, 3, 4], [3, 4, 5]]", printer.mask(new byte[][]{{1, 2, 3}, {2, 3, 4}, {3, 4, 5}}));
        Assertions.assertEquals("[[[1, 3]], [[2, 4]]]", printer.mask(new byte[][][]{{{1, 3}}, {{2, 4}}}));
        Assertions.assertEquals("[false, true]", printer.mask(new boolean[]{false, true}));
        Assertions.assertEquals("[c, b]", printer.mask(new char[]{'c', 'b'}));
        Assertions.assertEquals("[1, 2, 3]", printer.mask(new int[]{1, 2, 3}));
        Assertions.assertEquals("[1, 2, 3]", printer.mask(new long[]{1L, 2L, 3L}));
        Assertions.assertEquals("[1.0, 2.0, 3.0]", printer.mask(new double[]{1.0, 2.0, 3.0}));
    }

    @Test
    void testSanitizeObject() throws Exception{
        Map<String, Object> map = new HashMap<>();
        map.put("test1", 1);
        map.put("test2", "test");
        map.put("test3", false);
        map.put("example", example);
        example.depthObject.put("exampleMap", map);
        String result = printer.mask(map);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("***"));
        Assertions.assertTrue(result.contains("@ref["));
    }

    @Test
    void testSanitizeCycleMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", 1);
        map.put("test2", example);
        map.put("cycle", map);
        String result = printer.mask(map);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("***"));
        Assertions.assertTrue(result.contains("@ref["));
    }

    @Test
    void testSanitizeCycleObject() {
        example.setObjectSanitizePrinterExample(example);
        String result = printer.mask(example);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("***"));
        Assertions.assertTrue(result.contains("@ref["));
    }

    @Test
    void testSanitizeCycleObject2() {
        example.setObjectSanitizePrinterExample(example);
        Map<String, Object> map = new HashMap<>();
        map.put("test2", example);
        map.put("cycle", map);
        String result = printer.mask(map);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("***"));
        Assertions.assertTrue(result.contains("@ref["));
    }

    @Test
    void testConcurrency() throws Exception {
        Map<String, Object> map = new HashMap<>();
        List<String> texts = new ArrayList<>();
        map.put("texts", texts);
        map.put("test2", example);
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        Runnable putRunnable = () -> {
            map.put("test1", texts);
            texts.add("2");
            map.put("test3", texts);
        };
        Runnable printRunnable = () -> Assertions.assertNotNull(printer.mask(map));
        int maxCount = 10000;
        List<Future<?>> futures = new ArrayList<>(maxCount);
        for (int i = 0; i < maxCount; i++) {
            futures.add(executorService.submit(putRunnable));
            futures.add(executorService.submit(printRunnable));
        }
        for (Future<?> future : futures) {
            future.get();
        }
    }

    @Test
    void testSanitizeDepthObject() {
        Map<String, Object> depth1 = new HashMap<>();
        depth1.put("test2", example);
        example.setDepthObject(depth1);
        Map<String, Object> depth2 = new HashMap<>();
        depth1.put("depth2", depth2);
        Map<String, Object> depth3 = new HashMap<>();
        depth2.put("depth3", depth3);
        Map<String, Object> depth4 = new HashMap<>();
        depth3.put("depth4", depth4);
        ObjectSanitizePrinterExample objectSanitizePrinterExampleDepth1 = new ObjectSanitizePrinterExample();
        objectSanitizePrinterExampleDepth1.setAge(1);
        objectSanitizePrinterExampleDepth1.setAk("depth");
        example.setObjectSanitizePrinterExample(objectSanitizePrinterExampleDepth1);
        String result = printer.mask(example);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("***"));
        Assertions.assertTrue(result.contains("@ref["));
    }

    @Test
    void testPrintOverMaxSize() {
        Map<String, Object> map = new HashMap<>();
        List<String> list = new ArrayList<>();
        String[] array = new String[MAX_COLLECTION_SIZE + 1];
        for (int i = 0; i < MAX_COLLECTION_SIZE + 1; i++) {
            map.put(String.valueOf(i), i);
            list.add(String.valueOf(i));
            array[i] = String.valueOf(i);
        }
        Assertions.assertEquals("java.util.HashMap 对象的大小超过：128", printer.mask(map));
        Assertions.assertEquals("[Ljava.lang.Object; 对象的大小超过：128", printer.mask(list));
        Assertions.assertEquals("[Ljava.lang.String; 对象的大小超过：128", printer.mask(array));
    }

    @Test
    void testJavaFunctions() {
        Assertions.assertNotNull(printer.mask((Supplier<String>) () -> ""));
    }

    @Test
    void testMapObjects() {
        example.setSensitiveMaps(buildSensitiveMaps());
        String val = printer.mask(example);
        Assertions.assertTrue(val.contains("****"));
    }

    @Test
    void testJsonString() {
        example.setSensitiveText(JSON.toJSONString(buildSensitiveMaps()));
        String val = printer.mask(example);
        Assertions.assertTrue(val.contains("****"));
    }

    static Map<String, Object> buildSensitiveMaps() {
        Map<String, Object> sensitiveMaps = new HashMap<>();
        List<Map<String, String>> values = new ArrayList<>();
        Map<String, String> item = new HashMap<>();
        item.put("ak", "0001");
        values.add(item);
        sensitiveMaps.put("data", new HashMap<>(ImmutableMap.of("values", values)));
        return sensitiveMaps;
    }

    public static class ExampleObjectMasker implements ObjectMasker<Object, String> {

        @Override
        public String mask(Object obj, Collection<String> keys) {
            return ObjectMaskPrinter.ASTERISK.mask(obj);
        }
    }

    @Data
    @Sensitive(names = {"code", "ak"}, masker = ExampleObjectMasker.class)
    static class ObjectSanitizePrinterExample {

        @Sensitive(masker = ExampleObjectMasker.class)
        private String name;

        private String ak;

        private String code;

        private String desc;

        private int age;

        private boolean zed;

        private ObjectSanitizePrinterExample objectSanitizePrinterExample;

        private Map<String, Object> depthObject;

        @Sensitive(names = {"$.data.values[0].ak"}, masker = MapObjectMasker.class)
        private Map<String, Object> sensitiveMaps;

        @Sensitive(names = {"$.data.values[0].ak", "$.data.ak"}, masker = JsonStringMasker.class)
        private String sensitiveText;
    }
}
