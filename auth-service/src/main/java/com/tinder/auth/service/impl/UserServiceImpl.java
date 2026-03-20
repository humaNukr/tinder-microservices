package com.tinder.auth.service.impl;

import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.repository.UserRepository;
import com.tinder.auth.service.interfaces.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;

	@Override
	@Transactional
	public UserResult findOrCreateUser(String email) {
		Optional<User> existingUser = userRepository.findByEmail(email);
		if (existingUser.isPresent()) {
			return new UserResult(existingUser.get(), false);
		}

		User newUser = User.builder().email(email).role(User.Role.USER).isEmailVerified(true).build();
		User saved = userRepository.save(newUser);
		return new UserResult(saved, true);
	}

	@Override
	@Transactional(readOnly = true)
	public User findUserById(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
	}
}
