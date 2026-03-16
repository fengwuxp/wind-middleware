package com.wind.rocketmq;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.common.message.Message;

/**
 * 在消息发出之前在消息中增加 traceId
 *
 * @author wuxp
 * @date 2024-06-17 15:41
 **/
public final class SendMessageTraceHook implements SendMessageHook {

    @Override
    public String hookName() {
        return SendMessageTraceHook.class.getName();
    }

    @Override
    public void sendMessageBefore(SendMessageContext context) {
        addUserProperties(context.getMessage());
    }

    @Override
    public void sendMessageAfter(SendMessageContext context) {

    }

    private void addUserProperties(Message message) {
        if (!message.getProperties().containsKey(WindConstants.TRACE_ID_NAME)) {
            WindTracer.TRACER.currentTraceId().ifPresent(traceId -> message.putUserProperty(WindConstants.TRACE_ID_NAME, traceId));
        }
    }
}
