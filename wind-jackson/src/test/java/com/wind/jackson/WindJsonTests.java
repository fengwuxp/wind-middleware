package com.wind.jackson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.TypeFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindJsonTests {

    private JsonMapper originalMapper;

    @BeforeEach
    void setUp() {
        originalMapper = WindJson.getJsonMapper();
    }

    @AfterEach
    void tearDown() {
        WindJson.setJsonMapper(originalMapper);
    }

    @Test
    void testSerializeAndParseObjectUsingClass() {
        JsonSample sample = new JsonSample("wind", 2, SampleStatus.READY, null);

        String json = WindJson.toJsonString(sample);

        assertTrue(json.contains("\"optional\":null"));
        assertTrue(json.contains("\"status\":\"wire-ready\""));
        assertEquals(sample, WindJson.parseObject(json, JsonSample.class));
    }

    @Test
    void testSerializeAndParseNull() {
        assertEquals("null", WindJson.toJsonString(null));
        assertNull(WindJson.parseObject("null", JsonSample.class));
        assertNull(WindJson.parseArray("null", JsonSample.class));
        assertNull(WindJson.convertValue(null, JsonSample.class));
    }

    @Test
    void testPreserveNullMapValues() {
        assertEquals("{\"value\":null}", WindJson.toJsonString(Collections.singletonMap("value", null)));
    }

    @Test
    void testParseObjectUsingTypeReference() {
        List<JsonSample> values = WindJson.parseObject(
                "[{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\"}]",
                new TypeReference<List<JsonSample>>() {
                });

        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)), values);
    }

    @Test
    void testParseObjectUsingJavaType() {
        JavaType targetType = TypeFactory.createDefaultInstance().constructCollectionType(List.class, JsonSample.class);

        List<JsonSample> values = WindJson.parseObject(
                "[{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\"}]",
                targetType);

        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)), values);
    }

    @Test
    void testParseArrayUsingElementClass() {
        List<JsonSample> values = WindJson.parseArray(
                "[{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\"}]",
                JsonSample.class);

        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)), values);
    }

    @Test
    void testConvertValueUsingAllTypeForms() {
        Map<String, Object> source = Map.of("name", "wind", "count", 1, "status", "wire-ready");
        List<Map<String, Object>> sources = List.of(source);
        TypeReference<List<JsonSample>> typeReference = new TypeReference<>() {
        };
        JavaType javaType = TypeFactory.createDefaultInstance().constructType(typeReference);

        assertEquals(new JsonSample("wind", 1, SampleStatus.READY, null),
                WindJson.convertValue(source, JsonSample.class));
        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)),
                WindJson.convertValue(sources, typeReference));
        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)),
                WindJson.convertValue(sources, javaType));
    }

    @Test
    void testUseWindDateTimeFormats() {
        TimeSample value = new TimeSample(
                LocalDateTime.of(2026, 8, 3, 9, 8, 7),
                LocalDate.of(2026, 8, 3),
                LocalTime.of(9, 8, 7));

        String json = WindJson.toJsonString(value);

        assertTrue(json.contains("\"dateTime\":\"2026-08-03 09:08:07\""));
        assertTrue(json.contains("\"date\":\"2026-08-03\""));
        assertTrue(json.contains("\"time\":\"09:08:07\""));
        assertEquals(value, WindJson.parseObject(json, TimeSample.class));
    }

    @Test
    void testIgnoreUnknownPropertiesByJacksonDefault() {
        JsonSample value = WindJson.parseObject(
                "{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\",\"unknown\":true}",
                JsonSample.class);

        assertEquals(new JsonSample("wind", 1, SampleStatus.READY, null), value);
    }

    @Test
    void testDeserializeUntypedDecimalUsingJacksonDefault() {
        Map<String, Object> value = WindJson.parseObject(
                "{\"amount\":1234567890.12345678901234567890}",
                new TypeReference<Map<String, Object>>() {
                });

        assertInstanceOf(Double.class, value.get("amount"));
    }

    @Test
    void testRejectTrailingTokensByJacksonDefault() {
        assertThrows(DatabindException.class,
                () -> WindJson.parseObject("{\"count\":1}{}", PrimitiveSample.class));
    }

    @Test
    void testRejectDuplicateObjectKeys() {
        assertThrows(StreamReadException.class,
                () -> WindJson.parseObject("{\"count\":1,\"count\":2}", PrimitiveSample.class));
    }

    @Test
    void testRejectNullForPrimitiveByJacksonDefault() {
        assertThrows(DatabindException.class,
                () -> WindJson.parseObject("{\"count\":null}", PrimitiveSample.class));
    }

    @Test
    void testDeserializeEnumOrdinalUsingJacksonDefault() {
        assertEquals(new EnumSample(SampleStatus.READY),
                WindJson.parseObject("{\"status\":0}", EnumSample.class));
    }

    @Test
    void testRejectExcessiveNestingDepth() {
        String json = "[".repeat(201) + "0" + "]".repeat(201);

        assertThrows(StreamConstraintsException.class, () -> WindJson.parseObject(json, Object.class));
    }

    @Test
    void testRejectExcessiveNumberLength() {
        String json = "9".repeat(1001);

        assertThrows(StreamConstraintsException.class, () -> WindJson.parseObject(json, Object.class));
    }

    @Test
    void testRejectExcessivePropertyNameLength() {
        String json = "{\"" + "a".repeat(16 * 1024 + 1) + "\":true}";

        assertThrows(StreamConstraintsException.class, () -> WindJson.parseObject(json, Object.class));
    }

    @Test
    void testDoNotEnablePolymorphicTypeRestoration() {
        Object value = WindJson.parseObject(
                "{\"@class\":\"java.lang.Runtime\",\"value\":\"ignored\"}",
                Object.class);

        Map<?, ?> result = assertInstanceOf(Map.class, value);
        assertEquals("java.lang.Runtime", result.get("@class"));
    }

    @Test
    void testValidateJsonAndTargetArguments() {
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject(null, JsonSample.class));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject("  ", JsonSample.class));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject("{}", (Class<Object>) null));
        assertThrows(IllegalArgumentException.class,
                () -> WindJson.parseObject("{}", (TypeReference<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject("{}", (JavaType) null));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseArray("[]", null));
    }

    @Test
    void testPropagateNativeParseFailure() {
        assertThrows(StreamReadException.class,
                () -> WindJson.parseObject("not-json", JsonSample.class));
    }

    @Test
    void testPropagateNativeSerializationFailure() {
        assertThrows(DatabindException.class, () -> WindJson.toJsonString(new BrokenValue()));
    }

    @Test
    void testConfigureJsonMapperIncrementally() {
        JsonMapper before = WindJson.getJsonMapper();

        WindJson.configureJsonMapper(builder -> builder.addModule(customValueModule()));

        JsonMapper configured = WindJson.getJsonMapper();
        assertNotSame(before, configured);
        assertEquals("\"custom:wind\"", WindJson.toJsonString(new CustomValue("wind")));
        assertSecureFactoryConfiguration(configured);
        LocalDateTime dateTime = LocalDateTime.of(2026, 8, 3, 9, 8, 7);
        String dateTimeJson = WindJson.toJsonString(dateTime);
        assertEquals("\"2026-08-03 09:08:07\"", dateTimeJson);
        assertEquals(dateTime, WindJson.parseObject(dateTimeJson, LocalDateTime.class));
        assertEquals(new JsonSample("wind", 1, SampleStatus.READY, null), WindJson.parseObject(
                "{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\",\"unknown\":true}",
                JsonSample.class));
    }

    @Test
    void testConfigureJsonMapperCumulatively() {
        WindJson.configureJsonMapper(builder -> builder.enable(SerializationFeature.INDENT_OUTPUT));
        JsonMapper firstConfigured = WindJson.getJsonMapper();

        WindJson.configureJsonMapper(builder -> builder.addModule(customValueModule()));

        assertNotSame(firstConfigured, WindJson.getJsonMapper());
        assertTrue(WindJson.toJsonString(Map.of("value", 1)).contains("\n"));
        assertEquals("\"custom:wind\"", WindJson.toJsonString(new CustomValue("wind")));
    }

    @Test
    void testReplaceJsonMapperWithCustomNamingStrategy() {
        JsonMapper replacement = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();

        WindJson.setJsonMapper(replacement);

        assertSame(replacement, WindJson.getJsonMapper());
        String json = WindJson.toJsonString(new NamingSample("wind"));
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"display_name\""));
        assertEquals(new NamingSample("wind"),
                WindJson.parseObject("{\"display_name\":\"wind\"}", NamingSample.class));
    }

    @Test
    void testReplacementJsonMapperTakesOverFactoryConfiguration() {
        JsonMapper replacement = JsonMapper.builder().build();

        WindJson.setJsonMapper(replacement);

        assertFalse(replacement.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        assertEquals(new PrimitiveSample(2),
                WindJson.parseObject("{\"count\":1,\"count\":2}", PrimitiveSample.class));
    }

    @Test
    void testReplaceJsonMapperWithWindDateTimeModule() {
        JsonMapper replacement = JsonMapper.builder()
                .addModule(WindJacksonModules.iso8601LikeJavaTimeModule())
                .build();

        WindJson.setJsonMapper(replacement);

        TimeSample value = new TimeSample(
                LocalDateTime.of(2026, 8, 3, 9, 8, 7),
                LocalDate.of(2026, 8, 3),
                LocalTime.of(9, 8, 7));
        String json = WindJson.toJsonString(value);
        assertTrue(json.contains("\"dateTime\":\"2026-08-03 09:08:07\""));
        assertEquals(value, WindJson.parseObject(json, TimeSample.class));
    }

    @Test
    void testKeepCurrentMapperWhenConfigurationFails() {
        JsonMapper before = WindJson.getJsonMapper();

        assertThrows(IllegalStateException.class, () -> WindJson.configureJsonMapper(builder -> {
            builder.enable(SerializationFeature.INDENT_OUTPUT);
            throw new IllegalStateException("configuration failed");
        }));

        assertSame(before, WindJson.getJsonMapper());
    }

    @Test
    void testKeepCurrentMapperWhenMapperBuildFails() {
        JsonMapper before = WindJson.getJsonMapper();
        SimpleModule failingModule = new SimpleModule("failing-module") {
            @Override
            public void setupModule(JacksonModule.SetupContext context) {
                throw new IllegalStateException("build failed");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> WindJson.configureJsonMapper(builder -> builder.addModule(failingModule)));

        assertSame(before, WindJson.getJsonMapper());
    }

    @Test
    void testUseSecureDefaultFactoryConfiguration() {
        assertSecureFactoryConfiguration(WindJson.getJsonMapper());
    }

    private static void assertSecureFactoryConfiguration(JsonMapper mapper) {
        StreamReadConstraints constraints = mapper.tokenStreamFactory().streamReadConstraints();
        ErrorReportConfiguration errorConfiguration = mapper.tokenStreamFactory().errorReportConfiguration();

        assertFalse(mapper.isEnabled(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION));
        assertTrue(mapper.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        assertEquals(0, errorConfiguration.getMaxErrorTokenLength());
        assertEquals(0, errorConfiguration.getMaxRawContentLength());
        assertEquals(200, constraints.getMaxNestingDepth());
        assertEquals(16L * 1024 * 1024, constraints.getMaxDocumentLength());
        assertEquals(1_000_000, constraints.getMaxTokenCount());
        assertEquals(1_000, constraints.getMaxNumberLength());
        assertEquals(8 * 1024 * 1024, constraints.getMaxStringLength());
        assertEquals(16 * 1024, constraints.getMaxNameLength());
    }

    @Test
    void testValidateMapperConfigurationArguments() {
        assertThrows(NullPointerException.class, () -> WindJson.setJsonMapper(null));
        assertThrows(NullPointerException.class, () -> WindJson.configureJsonMapper(null));
    }

    private static SimpleModule customValueModule() {
        SimpleModule module = new SimpleModule("custom-module");
        module.addSerializer(CustomValue.class, new ValueSerializer<>() {
            @Override
            public void serialize(CustomValue value, JsonGenerator generator, SerializationContext context) {
                generator.writeString("custom:" + value.value());
            }
        });
        return module;
    }

    private enum SampleStatus {
        READY;

        @Override
        public String toString() {
            return "wire-ready";
        }
    }

    private record JsonSample(String name, int count, SampleStatus status, String optional) {
    }

    private record TimeSample(LocalDateTime dateTime, LocalDate date, LocalTime time) {
    }

    private record PrimitiveSample(int count) {
    }

    private record EnumSample(SampleStatus status) {
    }

    private record CustomValue(String value) {
    }

    private record NamingSample(String displayName) {
    }

    private static final class BrokenValue {

        public String getValue() {
            throw new IllegalStateException("sensitive-value");
        }
    }
}
