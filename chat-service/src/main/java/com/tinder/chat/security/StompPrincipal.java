package com.tinder.chat.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@RequiredArgsConstructor
@Getter
public class StompPrincipal implements Principal {
    private final String name;
}