package org.bk.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.bk.model.Message;
import org.bk.model.PubSubProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Service
public class CloudPubSubService implements PubSubService {

    private final PubSubTemplate pubSubTemplate;
    private final PubSubProperties pubSubProperties;
    private final ObjectMapper objectMapper;

    private Flux<Message> cachedStream;

    private final int CONCURRENCY = 5;

    public CloudPubSubService(PubSubTemplate pubSubTemplate,
                              PubSubProperties pubSubProperties,
                              ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.pubSubProperties = pubSubProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(Message message) {
        ByteString data = ByteString.copyFromUtf8(JsonUtils.writeValueAsString(message, objectMapper));
        PubsubMessage pubSubMessage = PubsubMessage.newBuilder().setData(data).build();
        return Mono.fromFuture(pubSubTemplate.publish(pubSubProperties.topic(), pubSubMessage).completable()).then();
    }

    @Override
    public Flux<Message> retrieve() {
        if (cachedStream == null) {
            cachedStream =  Flux
                    .<List<AcknowledgeablePubsubMessage>>generate(sink -> {
                        List<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages =
                                pubSubTemplate.pull(pubSubProperties.subscriberId(), CONCURRENCY, false);
                        sink.next(acknowledgeablePubsubMessages);
                    })
                    .retry()
                    .flatMapIterable(Function.identity())
                    .map(ackMessage -> {
                        String rawData = ackMessage.getPubsubMessage().getData().toStringUtf8();
                        ackMessage.ack();
                        return JsonUtils.readValue(rawData, Message.class, objectMapper);
                    }).publish().autoConnect();
        }
        return cachedStream;
    }
}
