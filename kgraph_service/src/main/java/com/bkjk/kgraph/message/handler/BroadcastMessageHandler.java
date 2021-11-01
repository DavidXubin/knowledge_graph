package com.bkjk.kgraph.message.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import net.sf.json.JSONObject;
import com.rabbitmq.client.Channel;
import com.bkjk.kgraph.service.PluginEngine;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class BroadcastMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastMessageHandler.class);

    @Autowired
    protected PluginEngine pluginEngine;

    @Value("${project.role}")
    private String serviceRole;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(), //fanout exchange bind random queue
            exchange = @Exchange(value = "kgraph_fanout_exchange", type = ExchangeTypes.FANOUT)
        )
    )
    @RabbitHandler
    public void process(Message message, Channel channel) throws IOException {

        logger.info("fanoutAReceiver  : " + message.toString());
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String msg = "";

        try {
            msg = new String(message.getBody(), "utf-8");
            JSONObject paramas = JSONObject.fromObject(msg);

            String category = paramas.getString("message_type").trim();
            if (category.equals("update_plugin")) {
                String pluginName = paramas.getString("plugin_name").trim();

                pluginEngine.reloadFromDB(pluginName);
            }

            channel.basicAck(deliveryTag, true);
        } catch (Exception e) {
            channel.basicReject(deliveryTag, false);
            logger.error("Fail to handle {} on [{}], reason: {}",
                    msg, message.getMessageProperties().getConsumerQueue(), e.toString());
        }
    }


}
