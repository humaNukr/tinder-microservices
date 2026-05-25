package com.tinder.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true, nullable = false)
	private String email;

	@Builder.Default
	@Column(nullable = false)
	private boolean isEmailVerified = false;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = "auth_provider", nullable = false, length = 20)
	private AuthProvider authProvider;

	@CreatedDate
	@Column(updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public static User createViaEmailOtp(String email) {
		return User.builder().email(email).role(Role.USER).isEmailVerified(true).authProvider(AuthProvider.EMAIL_OTP)
				.build();
	}

	public static User createViaGoogle(String email) {
		return User.builder().email(email).role(Role.USER).isEmailVerified(true).authProvider(AuthProvider.GOOGLE)
				.build();
	}

	public enum Role {
		USER, ADMIN
	}

	public enum AuthProvider {
		EMAIL_OTP, GOOGLE, PHONE
	}
}
