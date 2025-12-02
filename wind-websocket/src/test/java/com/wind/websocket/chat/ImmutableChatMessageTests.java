package com.wind.websocket.chat;


import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

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
        jsonMapper = JsonMapper.builder().build();
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
        Assertions.assertEquals(message.getFromUserId(), deserialized.getFromUserId());
        Assertions.assertEquals(message.getSessionId(), deserialized.getSessionId());
        Assertions.assertEquals(message.getBody().size(), deserialized.getBody().size());
        Assertions.assertEquals(message.getBody().getFirst().content(), deserialized.getBody().getFirst().content());
        Assertions.assertEquals(message.getGmtCreate(), deserialized.getGmtCreate());
        Assertions.assertEquals(message.getSequenceId(), deserialized.getSequenceId());
        Assertions.assertEquals(message.getMetadata().get("metaKey"), deserialized.getMetadata().get("metaKey"));
    }
}
