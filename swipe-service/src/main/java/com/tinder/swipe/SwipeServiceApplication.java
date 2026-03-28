package com.tinder.swipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
@EnableScheduling
public class SwipeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwipeServiceApplication.class, args);
    }

}
