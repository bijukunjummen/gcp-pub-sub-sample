package org.bk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Processes messages coming in from Pub/Sub topic
 */
@Component
public class MessageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessor.class);
    private final PubSubService pubSubService;

    public MessageProcessor(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @PostConstruct
    void process() {
        pubSubService.retrieve(message -> {
            if (message.id().equals("123")) {
                throw new RuntimeException("throwing a deliberate exception");
            }
            LOGGER.info("Processing message: {}", message.toString());
        });
        LOGGER.info("Triggered processing of messages");
    }
}
