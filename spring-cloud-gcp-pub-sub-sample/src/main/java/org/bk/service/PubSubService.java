package org.bk.service;

import org.bk.model.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Responsible for publishing and retrieving content from a Cloud Pub/Sub topic
 */
public interface PubSubService {
    String publish(Message message);

    void retrieve(Consumer<Message> processor);
}
