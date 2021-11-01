package com.bkjk.kgraph.message.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

/**
 * 做消息可靠性投递
 */
public class ConfirmCallBackHandler implements RabbitTemplate.ConfirmCallback {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmCallBackHandler.class);

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String correlationDataId = null;
        if (correlationData != null) {
            correlationDataId = correlationData.getId();
        }

        if (ack) {
            logger.info("CONFIRM: message delivery succeeded [correlationId: {}]", correlationDataId);
        } else {
            logger.warn("CONFIRM: message delivery failed [correlationId:{}, cause:{}]", correlationDataId, cause);
        }
    }
}

