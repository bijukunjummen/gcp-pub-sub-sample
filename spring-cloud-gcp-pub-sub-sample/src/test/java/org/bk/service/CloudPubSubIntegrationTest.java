package org.bk.service;

import org.bk.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;

@Testcontainers
@SpringBootTest
@ContextConfiguration(initializers = CloudPubSubIntegrationTest.PropertiesInitializer.class)
class CloudPubSubIntegrationTest {
    @Container
    private static final PubSubEmulatorContainer emulator =
            new PubSubEmulatorContainer(
                    DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators"));

    @Autowired
    private CloudPubSubService cloudPubSubService;

    @MockBean
    private MessageProcessor messageProcessor;

    @Test
    void testPubSubBasicWiring() throws Exception {
        Message message = new Message("id", "payload");
        cloudPubSubService.publish(message);

        CountDownLatch latch = new CountDownLatch(1);
        cloudPubSubService.retrieve(msg -> {
            latch.countDown();
        });
        latch.await();
    }

    static class PropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "pubsub.topic=sampletopic",
                    "pubsub.subscriber-id=subid",
                    "pubsub.project=sampleproj",
                    "spring.cloud.gcp.pubsub.emulator-host=" + emulator.getEmulatorEndpoint()
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
