package com.tinder.auth.service.impl;

import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.repository.UserRepository;
import com.tinder.auth.service.interfaces.OutboxService;
import com.tinder.auth.service.interfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final KafkaProperties kafkaProperties;

    @Override
    @Transactional
    public UserResult findOrCreateUser(String email) {
        return userRepository.findByEmail(email).map(user -> new UserResult(user, false)).orElseGet(() -> {
            try {
                User newUser = User.builder().email(email).role(User.Role.USER).isEmailVerified(true).build();
                return new UserResult(userRepository.save(newUser), true);
            } catch (DataIntegrityViolationException e) {
                User recoveredUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("Potential race condition failed to recover", e));
                return new UserResult(recoveredUser, false);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

        outboxService.saveEvent(kafkaProperties.userActivity(),
                new UserActivityEvent(UUID.randomUUID(), userId, ActivityType.DELETE_ACCOUNT, Instant.now()));
        userRepository.deleteById(userId);
    }
}
