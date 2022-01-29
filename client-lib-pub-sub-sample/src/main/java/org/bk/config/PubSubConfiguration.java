package org.bk.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class PubSubConfiguration {

    @Bean
    public Publisher topicPubisher(PubSubProperties pubSubProperties) {
        try {
            if (pubSubProperties.hostPort() != null) {
                String hostPort = pubSubProperties.hostPort();
                TopicName topicName = TopicName.ofProjectTopicName(pubSubProperties.project(), pubSubProperties.topic());
                ManagedChannel channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
                TransportChannelProvider channelProvider =
                        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
                CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
                TopicAdminClient topicAdminClient =
                        TopicAdminClient.create(
                                TopicAdminSettings.newBuilder()
                                        .setTransportChannelProvider(channelProvider)
                                        .setCredentialsProvider(credentialsProvider)
                                        .build());
                try {
                    topicAdminClient.createTopic(topicName);
                } catch (AlreadyExistsException alreadyExistsException) {
                    //ignore..
                }

                SubscriptionAdminClient subscriptionAdminClient =
                        SubscriptionAdminClient.create(
                                SubscriptionAdminSettings.newBuilder()
                                        .setTransportChannelProvider(channelProvider)
                                        .setCredentialsProvider(credentialsProvider)
                                        .build()
                        );

                try {
                    SubscriptionName subscriptionName = SubscriptionName.of(pubSubProperties.project(),
                            pubSubProperties.subscriberId());
                    subscriptionAdminClient
                            .createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance(), 60);
                } catch (AlreadyExistsException a) {
                    //ignore..
                }
                return Publisher.newBuilder(topicName)
                        .setEndpoint(hostPort)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
            }
            return Publisher.newBuilder(pubSubProperties.topic()).build();
        } catch (IOException e) {
            //Fail startup if Publisher does not get cleanly created..
            throw new RuntimeException(e);
        }
    }
}
