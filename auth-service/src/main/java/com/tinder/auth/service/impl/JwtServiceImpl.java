package com.tinder.auth.service.impl;

import com.tinder.auth.entity.User;
import com.tinder.auth.exception.AuthenticationFailedException;
import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.service.interfaces.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(User user) {
        return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail())
                .claim("role", user.getRole().name()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpirationMs()))
                .signWith(secretKey).compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        return Jwts.builder().subject(user.getId().toString()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpirationMs()))
                .signWith(secretKey).compact();
    }

    @Override
    public String extractUserId(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new AuthenticationFailedException("Invalid or expired JWT token");
        }
    }
}
