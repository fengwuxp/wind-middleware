package com.wind.mask;

import com.alibaba.fastjson2.JSON;
import com.wind.common.util.WindDeepCopyUtils;
import com.wind.mask.annotation.Sensitive;
import com.wind.mask.masker.json.JsonStringMasker;
import com.wind.mask.masker.json.MapObjectMasker;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author wuxp
 * @date 2024-08-02 15:18
 **/
class ObjectDataMaskerTests {

    private final MaskRuleRegistry registry = new MaskRuleRegistry();

    private final ObjectDataMasker maker = new ObjectDataMasker(registry, WindDeepCopyUtils::copy);

    private DefaultObjectSanitizerDemo1 demo1;

    @BeforeEach
    void setup() {
        demo1 = new DefaultObjectSanitizerDemo1();
        demo1.setSensitiveMaps(ObjectMaskPrinterTests.buildSensitiveMaps());
        demo1.setSensitiveText(JSON.toJSONString(ObjectMaskPrinterTests.buildSensitiveMaps()));

        List<MaskRuleGroup> groups = MaskRuleGroup.builder()
                .form(DefaultObjectSanitizerDemo2.class)
                .ofMaskerType(DefaultObjectSanitizerDemo2.Fields.sensitiveMaps2, MapObjectMasker.class, "$.data.values[0].ak")
                .ofMaskerType(DefaultObjectSanitizerDemo2.Fields.sensitiveText2, JsonStringMasker.class, "$.data.values[0].ak", "$.data.ak")
                .next(Map.class)
                .of(WindMasker.ASTERISK, "ak", "name")
                .build();

        registry.registerRules(groups);

    }

    @Test
    void testRequiredMask() {
        Assertions.assertTrue(registry.requireMask(DefaultObjectSanitizerDemo1.class));
        Assertions.assertTrue(registry.requireMask(DefaultObjectSanitizerDemo2.class));
    }

    @Test
    void testMask() {
        DefaultObjectSanitizerDemo1 result = maker.maskAs(demo1);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getSensitiveText().contains("***"));
    }

    @Test
    void testMask2() {
        DefaultObjectSanitizerDemo2 target = mockDemo2();
        DefaultObjectSanitizerDemo2 result2 = maker.maskAs(target);
        Assertions.assertNotNull(result2);
        Assertions.assertFalse(target.getSensitiveText2().contains("***"));
        Assertions.assertTrue(result2.getSensitiveText2().contains("***"));

        // clear sensitive rules
        registry.clearRules(DefaultObjectSanitizerDemo2.class);
        result2 = maker.maskAs(target);
        Assertions.assertNotNull(result2);
        Assertions.assertFalse(result2.getSensitiveText2().contains("***"));
    }

    @Test
    void testMask3() {
        Long result = maker.maskAs(1L);
        Assertions.assertEquals(1L, result);
    }

    @Test
    void testMask4() {
        List<DefaultObjectSanitizerDemo2> result = maker.maskAs(Collections.singletonList(mockDemo2()));
        Assertions.assertTrue(result.getFirst().getSensitiveText2().contains("***"));
    }

    @Test
    void testMask5() {
        Map<String, Object> result = maker.maskAs(ObjectMaskPrinterTests.buildSensitiveMaps());
        Assertions.assertFalse(result.toString().contains("***"));
    }

    private DefaultObjectSanitizerDemo2 mockDemo2() {
        DefaultObjectSanitizerDemo2 result = new DefaultObjectSanitizerDemo2();
        result.setSensitiveMaps2(ObjectMaskPrinterTests.buildSensitiveMaps());
        result.setSensitiveText2(JSON.toJSONString(ObjectMaskPrinterTests.buildSensitiveMaps()));
        return result;
    }

    @Data
    @Sensitive()
    public static class DefaultObjectSanitizerDemo1 {

        @Sensitive(names = {"$.data.values[0].ak"}, masker = MapObjectMasker.class)
        private Map<String, Object> sensitiveMaps;

        @Sensitive(names = {"$.data.values[0].ak", "$.data.ak"}, masker = JsonStringMasker.class)
        private String sensitiveText;
    }

    @Data
    @FieldNameConstants
    public static class DefaultObjectSanitizerDemo2 {

        private Map<String, Object> sensitiveMaps2;

        private String sensitiveText2;

        private DefaultObjectSanitizerDemo1 demo1;
    }
}
