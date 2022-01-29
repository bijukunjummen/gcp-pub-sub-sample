package org.bk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pubsub")
public record PubSubProperties(String topic, String subscriberId, String project, String hostPort) {
}
