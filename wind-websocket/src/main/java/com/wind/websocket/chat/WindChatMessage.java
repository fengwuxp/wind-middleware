package com.wind.websocket.chat;

import com.wind.websocket.core.WindSessionMessage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

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
public interface WindChatMessage extends WindSessionMessage {

    /**
     * @return 消息内容，可能有多个条
     */
    @NotEmpty
    List<ChatMessageContent> getBody();

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
