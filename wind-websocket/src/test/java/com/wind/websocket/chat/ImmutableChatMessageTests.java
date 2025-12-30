package com.wind.websocket.chat;


import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import static com.wind.common.WindDateFormatPatterns.HH_MM_SS;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD;
import static com.wind.common.WindDateFormatPatterns.YYYY_MM_DD_HH_MM_SS;

/**
 * @author wuxp
 * @date 2025-05-30 17:48
 **/
@Slf4j
@Disabled
class ImmutableChatMessageTests {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setup() {
        jsonMapper = JsonMapper.builder()
                .addModule(buildJavaTimeModule())
                .build();

    }

    @NonNull
    private static JacksonModule buildJavaTimeModule() {
        SimpleModule result = new SimpleModule();
        result.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        result.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        result.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        result.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        result.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        result.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        return result;
    }

    @Test
    void testSerializeDeserialize() throws Exception {
        ChatMessageContent content = new ChatMessageContent(
                ChatMessageContentType.TEXT,
                "Hello, world!",
                Collections.singletonMap("metaKey", "metaValue")
        );

        ImmutableChatMessage message = new ImmutableChatMessage(
                "msg-001",
                "userA",
                "session123",
                Arrays.asList(content, content),
                LocalDateTime.of(2025, 5, 30, 16, 0, 0),
                1L,
                Collections.singletonMap("metaKey", "metaValue")
        );

        // 序列化为 JSON
        String json = jsonMapper.writeValueAsString(message);
        log.info("Serialized JSON = {}", json);

        // 反序列化回对象
        ImmutableChatMessage deserialized = jsonMapper.readValue(json, ImmutableChatMessage.class);

        // 断言字段值正确
        Assertions.assertEquals(message.getId(), deserialized.getId());
        Assertions.assertEquals(message.getSenderId(), deserialized.getSenderId());
        Assertions.assertEquals(message.getSessionId(), deserialized.getSessionId());
        Assertions.assertEquals(message.getBody().size(), deserialized.getBody().size());
        Assertions.assertEquals(message.getBody().getFirst().content(), deserialized.getBody().getFirst().content());
        Assertions.assertEquals(message.getGmtCreate(), deserialized.getGmtCreate());
        Assertions.assertEquals(message.getSequenceId(), deserialized.getSequenceId());
        Assertions.assertEquals(message.getMetadata().get("metaKey"), deserialized.getMetadata().get("metaKey"));
    }
}
