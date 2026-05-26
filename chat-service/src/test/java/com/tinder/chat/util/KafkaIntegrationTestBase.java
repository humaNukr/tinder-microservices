package com.tinder.chat.util;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.test.context.TestPropertySource;

@EnableKafka
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")
public abstract class KafkaIntegrationTestBase extends IntegrationTestBase {}
