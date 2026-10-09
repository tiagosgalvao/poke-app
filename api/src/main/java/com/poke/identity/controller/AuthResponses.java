package com.poke.identity.controller;

import java.time.Instant;
import java.util.UUID;

final class AuthResponses {

	static final String BEARER = "Bearer";

	private AuthResponses() {
	}

	record UserResponse(UUID id, String username, String email, Instant createdAt) {
	}

	record TokenResponse(String accessToken, String tokenType, Instant expiresAt) {
	}
}
