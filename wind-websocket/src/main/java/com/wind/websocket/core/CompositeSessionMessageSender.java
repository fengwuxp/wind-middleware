package com.wind.websocket.core;

import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

/**
 * 组合会话消息发送者
 *
 * @author wuxp
 * @date 2025-12-17 09:47
 **/
@AllArgsConstructor
public class CompositeSessionMessageSender implements WindSessionMessageSender {

    private final Collection<WindSessionMessageSender> senders;

    @Override
    public void sendMessage(@NonNull WindSessionMessage message) {
        for (WindSessionMessageSender sender : senders) {
            if (sender.support(message.getSender())) {
                sender.sendMessage(message);
            }
        }
    }

    @Override
    public boolean support(@NonNull WindSessionMessageActor sender) {
        return true;
    }
}
