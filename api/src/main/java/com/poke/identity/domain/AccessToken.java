package com.poke.identity.domain;

import com.poke.shared.validation.Require;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

	public AccessToken {
		Require.text(value, "token");
		Require.present(expiresAt, "expires at");
	}
}
