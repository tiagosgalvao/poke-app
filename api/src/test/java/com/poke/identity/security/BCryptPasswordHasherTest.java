package com.poke.identity.security;

import com.poke.identity.domain.RawPassword;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

	private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

	@Test
	void hashesWithBCryptAndNeverStoresThePlainPassword() {
		var hash = hasher.hash(new RawPassword("Pikachu123!"));

		assertThat(hash).startsWith("$2a$").doesNotContain("Pikachu123!");
	}

	@Test
	void matchesOnlyTheOriginalPassword() {
		var hash = hasher.hash(new RawPassword("Pikachu123!"));

		assertThat(hasher.matches(new RawPassword("Pikachu123!"), hash)).isTrue();
		assertThat(hasher.matches(new RawPassword("Raichu1234!"), hash)).isFalse();
	}

	@Test
	void hashingTwiceGivesDifferentSaltedHashes() {
		var password = new RawPassword("Pikachu123!");

		assertThat(hasher.hash(password)).isNotEqualTo(hasher.hash(password));
	}
}
