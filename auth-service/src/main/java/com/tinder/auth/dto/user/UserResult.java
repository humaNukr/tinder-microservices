package com.tinder.auth.dto.user;

import com.tinder.auth.entity.User;

public record UserResult(User user, boolean isNew) {
}
