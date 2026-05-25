package com.tinder.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Value("${app.kafka.topics.user-activity}")
	private String userActivityTopic;

	@Bean
	public NewTopic userActivityEventsTopic() {
		return TopicBuilder.name(userActivityTopic).partitions(3).replicas(1).build();
	}
}
