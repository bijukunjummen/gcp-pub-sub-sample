package org.bk.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiService;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bk.config.PubSubProperties;
import org.bk.model.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CloudPubSubService implements PubSubService {

    private final Publisher publisher;
    private final ObjectMapper objectMapper;
    private final PubSubProperties pubSubProperties;

    public CloudPubSubService(Publisher publisher, ObjectMapper objectMapper, PubSubProperties pubSubProperties) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.pubSubProperties = pubSubProperties;
    }

    @Override
    public Mono<Void> publish(Message message) {
        ByteString data = ByteString.copyFromUtf8(JsonUtils.writeValueAsString(message, objectMapper));
        PubsubMessage pubSubMessage = PubsubMessage.newBuilder().setData(data).build();
        ApiFuture<String> messageIdFuture = publisher.publish(pubSubMessage);
        Mono<String> messageIdMono = ApiFutureUtil.toMono(messageIdFuture);
        return messageIdMono.then();
    }

    @Override
    public Flux<Message> retrieve() {
        // Purely for demonstration purpose..this is not backpressure safe and can overwhelm the jvm if there are
        // too many messages in the GCP topic
        return Flux.create(sink -> {
            MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
                String rawData = message.getData().toStringUtf8();
                sink.next(JsonUtils.readValue(rawData, Message.class, objectMapper));
                consumer.ack();
            };

            Subscriber subscriber = getSubscriber(receiver);
            ApiService.Listener listener = new ApiService.Listener() {
                @Override
                public void failed(ApiService.State from, Throwable failure) {
                    sink.error(failure);
                }

                @Override
                public void running() {
                    super.running();
                }

                @Override
                public void starting() {
                    super.starting();
                }

                @Override
                public void stopping(ApiService.State from) {
                    super.stopping(from);
                }

                @Override
                public void terminated(ApiService.State from) {
                    super.terminated(from);
                    sink.complete();
                }
            };
            subscriber.startAsync().addListener(listener, MoreExecutors.directExecutor());
        });
    }

    private Subscriber getSubscriber(MessageReceiver receiver) {
        if (pubSubProperties.hostPort() != null) {
            String hostPort = pubSubProperties.hostPort();
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
            ManagedChannel channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            return Subscriber.newBuilder(ProjectSubscriptionName
                            .newBuilder()
                            .setProject(pubSubProperties.project())
                            .setSubscription(pubSubProperties.subscriberId())
                            .build(), receiver)
                    .setEndpoint(hostPort)
                    .setCredentialsProvider(credentialsProvider)
                    .setChannelProvider(channelProvider)
                    .build();
        }
        return Subscriber.newBuilder(ProjectSubscriptionName
                        .newBuilder()
                        .setSubscription(pubSubProperties.subscriberId())
                        .build(), receiver)
                .build();

    }
}
