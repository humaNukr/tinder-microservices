package com.tinder.auth.service.impl;

import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.exception.UserNotFoundException;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.repository.UserRepository;
import com.tinder.auth.service.interfaces.OutboxService;
import com.tinder.auth.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final KafkaProperties kafkaProperties;

    @Override
    public UserResult findOrCreateUser(String email) {
        return userRepository.findByEmail(email).map(user -> new UserResult(user, false))
                .orElseGet(() -> createUserSafely(email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        outboxService.saveEvent(kafkaProperties.userActivity(),
                new UserActivityEvent(UUID.randomUUID(), userId, ActivityType.DELETE_ACCOUNT, Instant.now()));

        userRepository.deleteById(userId);
        log.info("User {} deleted and outbox event scheduled", userId);
    }

    private UserResult createUserSafely(String email) {
        try {
            User newUser = User.createNewVerifiedUser(email);
            return new UserResult(userRepository.save(newUser), true);

        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition detected while creating user with email: {}. Recovering...", email);

            User recoveredUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Potential race condition failed to recover", e));

            return new UserResult(recoveredUser, false);
        }
    }
}
