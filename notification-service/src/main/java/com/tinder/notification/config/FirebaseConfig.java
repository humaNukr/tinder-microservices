package com.tinder.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    @ConditionalOnResource(resources = "classpath:serviceAccountKey.json")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully");
            return app;
        }
    }

    @Bean
    @ConditionalOnResource(resources = "classpath:serviceAccountKey.json")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
