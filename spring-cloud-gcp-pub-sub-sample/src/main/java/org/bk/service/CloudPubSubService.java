package org.bk.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.reactive.PubSubReactiveFactory;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.bk.model.Message;
import org.bk.model.PubSubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Service
public class CloudPubSubService implements PubSubService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPubSubService.class);

    private final PubSubTemplate pubSubTemplate;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubProperties pubSubProperties;
    private final ObjectMapper objectMapper;

    public CloudPubSubService(PubSubTemplate pubSubTemplate,
                              PubSubAdmin pubSubAdmin, PubSubProperties pubSubProperties,
                              PubSubReactiveFactory pubSubReactiveFactory,
                              ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.pubSubAdmin = pubSubAdmin;
        this.pubSubProperties = pubSubProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String publish(Message message) {
        ByteString data = ByteString.copyFromUtf8(JsonUtils.writeValueAsString(message, objectMapper));
        PubsubMessage pubSubMessage = PubsubMessage.newBuilder().setData(data).build();
        try {
            return pubSubTemplate.publish(pubSubProperties.topic(), pubSubMessage).get();
        } catch (InterruptedException e) {
            throw new ServiceException(e);
        } catch (ExecutionException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public void retrieve(Consumer<Message> processor) {
        pubSubTemplate.subscribe(pubSubProperties.subscriberId(), consumerAck -> {
            try {
                String rawData = consumerAck.getPubsubMessage().getData().toStringUtf8();
                Message message = JsonUtils.readValue(rawData, Message.class, objectMapper);
                processor.accept(message);
            } finally {
                consumerAck.ack();
            }
        });
    }

    @PostConstruct
    void init() {
        Topic topic = pubSubAdmin.getTopic(pubSubProperties.topic());
        if (topic == null) {
            topic = pubSubAdmin.createTopic(pubSubProperties.topic());
        }

        Subscription subscription = pubSubAdmin.getSubscription(pubSubProperties.subscriberId());
        if (subscription == null) {
            subscription = pubSubAdmin.createSubscription(pubSubProperties.subscriberId(), topic.getName());
        }
        LOGGER.info("Topic {} and subscription {} is in place", topic.getName(), subscription.getName());
    }
}
