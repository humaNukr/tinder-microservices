package com.tinder.chat.chat.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.message.service.MessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioMediaEventProcessor {

    private final MessageFacade messageFacade;
    private final ObjectMapper objectMapper;

    public void processMinioMediaEvent(String minioEventJson) {
        try {
            JsonNode root = objectMapper.readTree(minioEventJson);

            String objectKey = extractObjectKeyFromJson(root);

            messageFacade.confirmMediaUpload(objectKey);

        } catch (Exception e) {
            log.error("Error processing MinIO event. JSON: {}", minioEventJson, e);
        }
    }

    private String extractObjectKeyFromJson(JsonNode json) {
        String encodedKey = json.get("Records").get(0).get("s3").get("object").get("key").asText();

        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }
}
