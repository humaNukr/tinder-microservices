package com.tinder.auth.service.impl;

import com.tinder.auth.entity.User;
import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.service.interfaces.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

	private final JwtProperties jwtProperties;

	public String generateAccessToken(User user) {
		return Jwts.builder().subject(user.getId().toString()).claim("email", user.getEmail())
				.claim("role", user.getRole().name()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpirationMs()))
				.signWith(getSignInKey()).compact();
	}

	public String generateRefreshToken(User user) {
		return Jwts.builder().subject(user.getId().toString()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpirationMs()))
				.signWith(getSignInKey()).compact();
	}

	public String extractUserId(String token) {
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload().getSubject();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
