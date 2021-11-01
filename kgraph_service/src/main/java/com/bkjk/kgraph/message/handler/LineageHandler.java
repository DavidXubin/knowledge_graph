package com.bkjk.kgraph.message.handler;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Component
public class LineageHandler extends GraphMessageHandler {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        super.onMessage(message, channel);
    }

}
