package com.tinder.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
        String url,
        String accessKey,
        String secretKey,
        String bucketName
) {
}
