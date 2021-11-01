package com.bkjk.kgraph.message.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 消息路由失败
 */
public class ReturnCallBackHandler implements RabbitTemplate.ReturnCallback {

    private static final Logger logger = LoggerFactory.getLogger(ReturnCallBackHandler.class);

    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        String msg = new String(message.getBody());
        logger.warn("RETURN: message routing failed [msg:{}, replyCode:{}, replyText:{}, exchange:{}, routingKey:{}]",
                msg, replyCode, replyText, exchange, routingKey);
    }

}
