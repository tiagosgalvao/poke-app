package com.poke.identity.domain;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.validation.Require;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public record User(UUID id, String username, String email, String passwordHash, Instant createdAt) {

	private static final Pattern USERNAME = Pattern.compile("[a-z0-9_.-]{3,30}");
	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	public User {
		Require.present(id, "id");
		if (username == null || !USERNAME.matcher(username).matches()) {
			throw new DomainValidationException(
					"username must be 3 to 30 lowercase letters, digits, dots, hyphens or underscores");
		}
		if (email == null || !EMAIL.matcher(email).matches()) {
			throw new DomainValidationException("email must be a valid address");
		}
		Require.text(passwordHash, "password hash");
		Require.present(createdAt, "created at");
	}

	public static User register(String username, String email, String passwordHash, Instant now) {
		return new User(UUID.randomUUID(), normalized(username), normalized(email), passwordHash, now);
	}

	public static String normalized(String value) {
		return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
	}
}
