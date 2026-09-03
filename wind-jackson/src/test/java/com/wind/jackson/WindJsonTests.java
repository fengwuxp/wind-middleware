package com.wind.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.TypeFactory;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    void testParseObjectUsingReflectType() {
        Type targetType = new TypeReference<List<JsonSample>>() {
        }.getType();

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
    void testParseArrayUsingAllTypeForms() {
        String json = "[{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\"}]";
        TypeReference<List<JsonSample>> typeReference = new TypeReference<>() {
        };
        JavaType javaType = TypeFactory.createDefaultInstance().constructType(typeReference);
        Type reflectType = typeReference.getType();
        List<JsonSample> expected = List.of(new JsonSample("wind", 1, SampleStatus.READY, null));

        assertEquals(expected, WindJson.parseArray(json, typeReference));
        assertEquals(expected, WindJson.parseArray(json, reflectType));
        assertEquals(expected, WindJson.parseArray(json, javaType));
    }

    @Test
    void testParseTreePreservesJsonNodeTypes() {
        JsonNode tree = WindJson.parseTree("{\"items\":[{\"id\":1}],\"none\":null}");

        assertTrue(tree.isObject());
        assertTrue(tree.path("items").isArray());
        assertEquals(1, tree.at("/items/0/id").intValue());
        assertTrue(tree.path("none").isNull());
        assertTrue(WindJson.parseTree("null").isNull());
    }

    @Test
    void testParseTreeRejectsMalformedJson() {
        assertThrows(StreamReadException.class, () -> WindJson.parseTree("not-json"));
        assertThrows(DatabindException.class, () -> WindJson.parseTree("{}{}"));
    }

    @Test
    void testSerializeToJsonBytesUsesUtf8() {
        byte[] bytes = WindJson.toJsonBytes(Map.of("message", "测试"));

        assertEquals("{\"message\":\"测试\"}", new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("null", new String(WindJson.toJsonBytes(null), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void testConvertValueUsingAllTypeForms() {
        Map<String, Object> source = Map.of("name", "wind", "count", 1, "status", "wire-ready");
        List<Map<String, Object>> sources = List.of(source);
        TypeReference<List<JsonSample>> typeReference = new TypeReference<>() {
        };
        JavaType javaType = TypeFactory.createDefaultInstance().constructType(typeReference);
        Type reflectType = typeReference.getType();

        assertEquals(new JsonSample("wind", 1, SampleStatus.READY, null),
                WindJson.convertValue(source, JsonSample.class));
        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)),
                WindJson.convertValue(sources, typeReference));
        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)),
                WindJson.convertValue(sources, javaType));
        assertEquals(List.of(new JsonSample("wind", 1, SampleStatus.READY, null)),
                WindJson.convertValue(sources, reflectType));
    }

    @Test
    void testConvertJsonContainerTextUsingAllTypeForms() {
        String json = "{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\"}";
        TypeReference<JsonSample> typeReference = new TypeReference<>() {
        };
        TypeReference<List<JsonSample>> listTypeReference = new TypeReference<>() {
        };
        JavaType javaType = TypeFactory.createDefaultInstance().constructType(typeReference);
        Type reflectType = typeReference.getType();
        JsonSample expected = new JsonSample("wind", 1, SampleStatus.READY, null);

        assertEquals(expected, WindJson.convertValue(json, JsonSample.class));
        assertEquals(expected, WindJson.convertValue(json, typeReference));
        assertEquals(expected, WindJson.convertValue(json, javaType));
        assertEquals(expected, WindJson.convertValue(json, reflectType));
        assertEquals(List.of(expected), WindJson.convertValue("[" + json + "]", listTypeReference));
        assertEquals("plain text", WindJson.convertValue("plain text", String.class));
        assertEquals(json, WindJson.convertValue(json, Object.class));
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
    void testParseWindDateTimeWithFractionalSeconds() {
        TimeSample value = WindJson.parseObject(
                "{\"dateTime\":\"2026-08-03 09:08:07.123456789\",\"date\":\"2026-08-03\",\"time\":\"09:08:07\"}",
                TimeSample.class);

        assertEquals(LocalDateTime.of(2026, 8, 3, 9, 8, 7, 123456789), value.dateTime());
    }

    @Test
    void testSerializeWindDateTimeWithFractionalSeconds() {
        LocalDateTime value = LocalDateTime.of(2026, 8, 3, 9, 8, 7, 123456789);

        String json = WindJson.toJsonString(value);

        assertEquals("\"2026-08-03 09:08:07.123456789\"", json);
        assertEquals(value, WindJson.parseObject(json, LocalDateTime.class));
    }

    @Test
    void testParseIsoLocalDateTimeWithFractionalSeconds() {
        TimeSample value = WindJson.parseObject(
                "{\"dateTime\":\"2026-08-03T09:08:07.123456789\",\"date\":\"2026-08-03\",\"time\":\"09:08:07\"}",
                TimeSample.class);

        assertEquals(LocalDateTime.of(2026, 8, 3, 9, 8, 7, 123456789), value.dateTime());
    }

    @Test
    void testParseIsoOffsetDateTimeAndInstant() {
        OffsetTimeSample value = WindJson.parseObject(
                "{\"offset\":\"2026-08-03T09:08:07.123456789+08:00\",\"instant\":\"2026-08-03T01:08:07.123456789Z\"}",
                OffsetTimeSample.class);

        assertEquals(
                OffsetDateTime.of(2026, 8, 3, 9, 8, 7, 123456789, ZoneOffset.ofHours(8)).toInstant(),
                value.offset().toInstant());
        assertEquals(Instant.parse("2026-08-03T01:08:07.123456789Z"), value.instant());
    }

    @Test
    void testDoNotCoerceOffsetDateTimeIntoLocalDateTime() {
        assertThrows(DatabindException.class,
                () -> WindJson.parseObject(
                        "{\"dateTime\":\"2026-08-03T09:08:07.123456789+08:00\",\"date\":\"2026-08-03\",\"time\":\"09:08:07\"}",
                        TimeSample.class));
    }

    @Test
    void testRejectLocalDateTimeWithoutSeconds() {
        assertThrows(DatabindException.class,
                () -> WindJson.parseObject(
                        "{\"dateTime\":\"2026-08-03T09:08\",\"date\":\"2026-08-03\",\"time\":\"09:08:00\"}",
                        TimeSample.class));
    }

    @Test
    void testParseCompactLocalTimeWithPropertyFormat() {
        CompactTimeSample value = WindJson.parseObject(
                "{\"time\":\"33454\"}", CompactTimeSample.class);

        assertEquals(LocalTime.of(3, 34, 54), value.getTime());
    }

    @Test
    void testIgnoreUnknownPropertiesByJacksonDefault() {
        JsonSample value = WindJson.parseObject(
                "{\"name\":\"wind\",\"count\":1,\"status\":\"wire-ready\",\"unknown\":true}",
                JsonSample.class);

        assertEquals(new JsonSample("wind", 1, SampleStatus.READY, null), value);
    }

    @Test
    void testComputedTypePropertyAllowsConcreteSubtypeWithoutTypeId() {
        ComputedWebhookPayload value = WindJson.parseObject(
                "{\"payload\":\"data\"}", ComputedWebhookPayload.class);

        assertEquals("data", value.getPayload());
    }

    @Test
    void testComputedTypePropertyStillRequiresTypeIdForBaseType() {
        assertThrows(InvalidTypeIdException.class,
                () -> WindJson.parseObject("{\"payload\":\"data\"}", ComputedWebhook.class));
    }

    @Test
    void testComputedTypePropertyAllowsUnknownTypeIdForConcreteSubtype() {
        ComputedWebhookPayload value = WindJson.parseObject(
                "{\"webhookType\":\"OTHER\",\"payload\":\"data\"}", ComputedWebhookPayload.class);

        assertEquals("data", value.getPayload());
    }

    @Test
    void testComputedTypePropertyRejectsKnownDifferentSubtypeForConcreteSubtype() {
        assertThrows(InvalidTypeIdException.class,
                () -> WindJson.parseObject(
                        "{\"webhookType\":\"OTHER_PAYLOAD\",\"payload\":\"data\"}",
                        ComputedWebhookPayload.class));
    }

    @Test
    void testComputedTypePropertyStillRejectsUnknownTypeIdForBaseType() {
        assertThrows(InvalidTypeIdException.class,
                () -> WindJson.parseObject(
                        "{\"webhookType\":\"OTHER\",\"payload\":\"data\"}", ComputedWebhook.class));
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
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject("{}", (Type) null));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseObject("{}", (JavaType) null));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseArray("[]", (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> WindJson.parseTree("  "));
        assertThrows(IllegalArgumentException.class, () -> WindJson.convertValue(null, (Type) null));
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

    private record OffsetTimeSample(OffsetDateTime offset, Instant instant) {
    }

    private static final class CompactTimeSample {

        @JsonFormat(pattern = "HH:mm")
        private LocalTime time;

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }
    }

    private record PrimitiveSample(int count) {
    }

    private record EnumSample(SampleStatus status) {
    }

    private record CustomValue(String value) {
    }

    private record NamingSample(String displayName) {
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "webhookType"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ComputedWebhookPayload.class, name = "PAYLOAD"),
            @JsonSubTypes.Type(value = OtherComputedWebhookPayload.class, name = "OTHER_PAYLOAD")
    })
    private interface ComputedWebhook {

        @JsonProperty(value = "webhookType", access = JsonProperty.Access.READ_ONLY)
        String getWebhookType();
    }

    private static final class ComputedWebhookPayload implements ComputedWebhook {

        private String payload;

        @Override
        public String getWebhookType() {
            return "PAYLOAD";
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

    private static final class OtherComputedWebhookPayload implements ComputedWebhook {

        private String payload;

        @Override
        public String getWebhookType() {
            return "OTHER_PAYLOAD";
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

    private static final class BrokenValue {

        public String getValue() {
            throw new IllegalStateException("sensitive-value");
        }
    }
}
