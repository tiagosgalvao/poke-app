package com.poke.identity.controller;

import com.poke.identity.controller.AuthResponses.TokenResponse;
import com.poke.identity.controller.AuthResponses.UserResponse;
import com.poke.identity.domain.AccessToken;
import com.poke.identity.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.poke.identity.controller.AuthResponses.BEARER;
import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseMapperTest {

	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

	private final AuthResponseMapper mapper = new AuthResponseMapperImpl();

	@Test
	void mapsAUserWithoutItsPasswordHash() {
		var id = UUID.randomUUID();
		var user = new User(id, "misty", "misty@cerulean.city", "hash", NOW);

		assertThat(mapper.toUserResponse(user)).isEqualTo(new UserResponse(id, "misty", "misty@cerulean.city", NOW));
	}

	@Test
	void mapsATokenAsABearerToken() {
		assertThat(mapper.toTokenResponse(new AccessToken("jwt", NOW))).isEqualTo(new TokenResponse("jwt", BEARER, NOW));
	}
}
