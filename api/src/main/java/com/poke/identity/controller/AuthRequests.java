package com.poke.identity.controller;

import com.poke.identity.domain.RawPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.poke.identity.domain.RawPassword.MAX_LENGTH;
import static com.poke.identity.domain.RawPassword.MIN_LENGTH;

final class AuthRequests {

	static final String USERNAME_PATTERN = "\\s*[A-Za-z0-9_.-]{3,30}\\s*";
	static final String USERNAME_MESSAGE = "must be 3 to 30 letters, digits, dots, hyphens or underscores";
	static final int MAX_EMAIL_LENGTH = 254;

	private AuthRequests() {
	}

	record RegisterRequest(
			@NotBlank @Pattern(regexp = USERNAME_PATTERN, message = USERNAME_MESSAGE) String username,
			@NotBlank @Email @Size(max = MAX_EMAIL_LENGTH) String email,
			@NotNull @Size(min = MIN_LENGTH, max = MAX_LENGTH) String password) {

		RawPassword rawPassword() {
			return new RawPassword(password);
		}
	}

	record LoginRequest(@NotBlank String username, @NotNull @Size(min = MIN_LENGTH, max = MAX_LENGTH) String password) {

		RawPassword rawPassword() {
			return new RawPassword(password);
		}
	}
}
