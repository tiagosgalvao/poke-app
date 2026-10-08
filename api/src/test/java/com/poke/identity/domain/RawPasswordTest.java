package com.poke.identity.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static com.poke.identity.domain.RawPassword.MAX_LENGTH;
import static com.poke.identity.domain.RawPassword.MIN_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

	@Test
	void acceptsPasswordsWithinTheAllowedLength() {
		assertThat(new RawPassword("a".repeat(MIN_LENGTH)).value()).hasSize(MIN_LENGTH);
		assertThat(new RawPassword("a".repeat(MAX_LENGTH)).value()).hasSize(MAX_LENGTH);
	}

	@Test
	void rejectsTooShortTooLongOrMissingPasswords() {
		assertThatThrownBy(() -> new RawPassword("a".repeat(MIN_LENGTH - 1))).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new RawPassword("a".repeat(MAX_LENGTH + 1))).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new RawPassword(null)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void neverPrintsTheSecret() {
		assertThat(new RawPassword("Pikachu123!")).hasToString("RawPassword[******]");
	}
}
