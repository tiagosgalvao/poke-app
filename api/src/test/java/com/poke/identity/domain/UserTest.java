package com.poke.identity.domain;

import com.poke.shared.exception.ConflictException;
import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

	@Test
	void registeringNormalizesUsernameAndEmailAndAssignsAnId() {
		var user = User.register("  Ash_Ketchum ", " Ash@Pallet.Town ", "hash", NOW);

		assertThat(user.id()).isNotNull();
		assertThat(user.username()).isEqualTo("ash_ketchum");
		assertThat(user.email()).isEqualTo("ash@pallet.town");
		assertThat(user.passwordHash()).isEqualTo("hash");
		assertThat(user.createdAt()).isEqualTo(NOW);
	}

	@Test
	void eachRegistrationGetsItsOwnId() {
		assertThat(User.register("ash", "ash@pallet.town", "hash", NOW).id())
			.isNotEqualTo(User.register("ash", "ash@pallet.town", "hash", NOW).id());
	}

	@Test
	void rejectsUsernamesOutsideTheAllowedShape() {
		assertThatThrownBy(() -> User.register("ab", "a@b.c", "hash", NOW)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> User.register("a".repeat(31), "a@b.c", "hash", NOW)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> User.register("ash ketchum", "a@b.c", "hash", NOW)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void rejectsMalformedEmails() {
		assertThatThrownBy(() -> User.register("ash", "not-an-email", "hash", NOW)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> User.register("ash", "a@b", "hash", NOW)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void rejectsMissingUsernameOrEmail() {
		assertThatThrownBy(() -> User.register(null, "a@b.c", "hash", NOW)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> User.register("ash", null, "hash", NOW)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void requiresAPasswordHash() {
		assertThatThrownBy(() -> User.register("ash", "ash@pallet.town", " ", NOW)).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void conflictsAndBadCredentialsHaveTheRightKind() {
		assertThat(new UsernameAlreadyTakenException("ash")).isInstanceOf(ConflictException.class)
			.hasMessageContaining("ash");
		assertThat(new EmailAlreadyRegisteredException()).isInstanceOf(ConflictException.class);
		assertThat(new InvalidCredentialsException()).isInstanceOf(UnauthorizedException.class)
			.hasMessage("Invalid username or password");
	}
}
