package com.bkjk.kgraph.message.handler;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.bkjk.kgraph.service.PluginEngine;
import com.bkjk.kgraph.service.PluginProperties;


@Component
public class RpcMessageHandler implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RpcMessageHandler.class);

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    protected PluginEngine pluginEngine;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        logger.info("RPC Receiver  : " + message.toString());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String msg = "";

        try {
            JSONObject params = JSONObject.fromObject(new String(message.getBody()));

            String query = params.getString("query").trim();

            JSONArray results = new JSONArray();

            if (query.equals("results")) {
                List pluginResults = pluginEngine.run(params);

                results.addAll(pluginResults);
            } else {
                String pluginName = params.getString("plugin_name");

                PluginProperties.Status status = pluginEngine.getPluginStatus(pluginName);

                results.add(status.name());

            }

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader("kgraph_correlation_id",  message.getMessageProperties().getHeaders().get("kgraph_correlation_id"));
            Message replyMessage = new Message(results.toString().getBytes(), props);

            //CorrelationData corrData = new CorrelationData((String) message.getMessageProperties().getHeaders().get("kgraph_correlation_id"));

            rabbitTemplate.send(message.getMessageProperties().getReplyTo(), replyMessage);

            channel.basicAck(deliveryTag, true);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, false);
            logger.error("Fail to handle {} on [{}], reason: {}",
                    msg, message.getMessageProperties().getConsumerQueue(), e.toString());
        }
    }
}
