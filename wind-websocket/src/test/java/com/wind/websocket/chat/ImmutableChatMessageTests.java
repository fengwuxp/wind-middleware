package com.wind.websocket.chat;


import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS)));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(YYYY_MM_DD)));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(HH_MM_SS)));
        jsonMapper = JsonMapper.builder()
                .addModule(module)
                .build();

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
