package com.tinder.chat.util;

import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public final class KafkaListenerAwait {

    private KafkaListenerAwait() {}

    public static void awaitAssignment(ApplicationContext applicationContext, String groupId) {
        KafkaListenerEndpointRegistry registry =
                applicationContext.getBean(KafkaListenerEndpointRegistry.class);

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> registry.getListenerContainers().stream()
                        .filter(container -> groupId.equals(container.getGroupId()))
                        .anyMatch(MessageListenerContainer::isRunning));
    }
}
