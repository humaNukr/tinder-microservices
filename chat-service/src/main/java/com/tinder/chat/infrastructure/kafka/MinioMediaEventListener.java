package com.tinder.chat.infrastructure.kafka;

import com.tinder.chat.chat.processor.MinioMediaEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioMediaEventListener {
    private final MinioMediaEventProcessor eventProcessor;

    @KafkaListener(
            topics = "${app.kafka.topics.minio-chat-media-events}",
            groupId = "${app.kafka.consumer-groups.chat-service}"
    )
    public void listenMinioMediaEvent(String minioEventJson) {
        eventProcessor.processMinioMediaEvent(minioEventJson);
    }
}