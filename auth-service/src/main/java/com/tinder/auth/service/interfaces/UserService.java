package com.tinder.auth.service.interfaces;

import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;

import java.util.UUID;

public interface UserService {
	UserResult findOrCreateUser(String email);

	User findUserById(UUID id);

	void deleteUser(UUID userId);
}
