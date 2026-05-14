package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.in.media.ConfirmMediaUploadUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioMediaEventKafkaAdapter {

    private final ConfirmMediaUploadUseCase confirmMediaUploadUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.minio-chat-media-events}",
            groupId = "${app.kafka.consumer-groups.chat-service}"
    )
    public void listenAndProcessMinioEvent(ConsumerRecord<String, String> record) {
        try {
            String minioEventJson = record.value();
            JsonNode root = objectMapper.readTree(minioEventJson);

            String objectKey = extractObjectKeyFromJson(root);

            confirmMediaUploadUseCase.confirmMediaUpload(objectKey);

        } catch (Exception e) {
            log.error("Error processing MinIO event. JSON: {}", record.value(), e);
        }
    }

    private String extractObjectKeyFromJson(JsonNode json) {
        String encodedKey = json.get("Records").get(0).get("s3").get("object").get("key").asText();
        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }
}