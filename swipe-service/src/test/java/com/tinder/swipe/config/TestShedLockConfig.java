package com.tinder.swipe.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

@Configuration
@Profile("test")
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class TestShedLockConfig {

    @Bean
    @Primary
    public LockProvider lockProvider() {
        return (LockConfiguration lockConfiguration) -> Optional.of(new SimpleLock() {
            @Override
            public void unlock() {
                // no-op for tests
            }
        });
    }
}
