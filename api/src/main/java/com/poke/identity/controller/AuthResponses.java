package com.poke.identity.controller;

import com.poke.identity.domain.AccessToken;
import com.poke.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

final class AuthResponses {

	static final String BEARER = "Bearer";

	private AuthResponses() {
	}

	record UserResponse(UUID id, String username, String email, Instant createdAt) {

		static UserResponse from(User user) {
			return new UserResponse(user.id(), user.username(), user.email(), user.createdAt());
		}
	}

	record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {

		static TokenResponse from(AccessToken token) {
			return new TokenResponse(token.value(), BEARER, token.expiresAt());
		}
	}
}
