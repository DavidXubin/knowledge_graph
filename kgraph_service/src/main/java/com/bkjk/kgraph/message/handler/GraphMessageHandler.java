package com.bkjk.kgraph.message.handler;

import com.bkjk.kgraph.service.PluginEngine;
import com.rabbitmq.client.Channel;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


public class GraphMessageHandler implements ChannelAwareMessageListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected PluginEngine pluginEngine;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String msg = "";

        try {
            msg = new String(message.getBody(), "utf-8");
            JSONObject params = JSONObject.fromObject(msg);

            MessageProperties pros = message.getMessageProperties();
            String routingKey = pros.getReceivedRoutingKey();

            logger.info("Receive message: {}", routingKey);

            List results = pluginEngine.run(params);
            logger.info(results.toString());

            channel.basicAck(deliveryTag, true);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, false);
            logger.error("Fail to handle {} on [{}], reason: {}",
                    msg, message.getMessageProperties().getConsumerQueue(), e.toString());
        }
    }
}
