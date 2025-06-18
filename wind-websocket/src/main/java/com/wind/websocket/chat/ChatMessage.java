package com.wind.websocket.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 所有的聊天都关联一个会话（即使是 2 个人），通过通过会话对其他消息接收者进行消息广播
 * Client A           Server                    Client B/C/D
 * |                  |                             |
 * |— sendMessage() —>|                             |
 * |  {sessionId,                                    |
 * |   from:A,                                      |
 * |   content:"Hi"}                                |
 * |                  |                             |
 * |                  |— validateSession(sessionId)->
 * |                  |                             |
 * |                  |— persistMessage(msg) —>DB   |
 * |                  |                             |
 * |                  |— lookupMembers(sessionId) —>DB
 * |                  |      => [A,B,C,D]           |
 * |                  |                             |
 * |                  |— for each member ≠ A        |
 * |                  |      pushTo(member, msg)    |
 * |                  |                             |
 * |                  |<— ACK “delivered” to A      |
 * |<— onAck() ————|                             |
 * |                  |                             |
 *
 * @author wuxp
 * @date 2025-05-27 10:28
 **/
public interface ChatMessage {

    /**
     * @return 消息 id
     */
    @NotBlank
    String getId();

    /**
     * @return 发送者
     */
    @NotBlank
    String getFromUserId();

    /**
     * 聊天会话 id
     *
     * @return 接收者
     */
    @NotBlank
    String getSessionId();

    /**
     * @return 消息内容，可能有多个条
     */
    @NotEmpty
    List<ChatMessageContent> getBody();

    /**
     * @return 发送时间戳
     */
    @NotNull
    default Long getTimestamp() {
        return getGmtCreate().toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    /**
     * @return 创建时间
     */
    @NotBlank
    LocalDateTime getGmtCreate();

    /**
     * @return 消息序列号
     */
    @NotNull
    Long getSequenceId();

    /**
     * @return 元数据
     */
    default Map<String, String> getMetadata() {
        return Collections.emptyMap();
    }

}
