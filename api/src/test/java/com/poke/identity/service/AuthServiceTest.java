package com.poke.identity.service;

import com.poke.identity.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
	private static final RawPassword PASSWORD = new RawPassword("Pikachu123!");
	private static final String HASH = "$2a$10$hash";

	@Mock
	UserRepository users;

	@Mock
	PasswordHasher hasher;

	@Mock
	TokenIssuer tokenIssuer;

	AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(users, hasher, tokenIssuer, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registersANewUserWithAHashedPassword() {
		when(hasher.hash(PASSWORD)).thenReturn(HASH);
		when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var ash = service.register(" Ash ", "Ash@Pallet.Town", PASSWORD);

		assertThat(ash.username()).isEqualTo("ash");
		assertThat(ash.email()).isEqualTo("ash@pallet.town");
		assertThat(ash.passwordHash()).isEqualTo(HASH);
		assertThat(ash.createdAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsATakenUsernameCaseInsensitively() {
		when(users.existsByUsername("ash")).thenReturn(true);

		assertThatThrownBy(() -> service.register("ASH", "new@pallet.town", PASSWORD))
			.isInstanceOf(UsernameAlreadyTakenException.class);
		verify(users, never()).save(any());
	}

	@Test
	void rejectsAnAlreadyRegisteredEmail() {
		when(users.existsByEmail("ash@pallet.town")).thenReturn(true);

		assertThatThrownBy(() -> service.register("ash2", "ASH@pallet.town", PASSWORD))
			.isInstanceOf(EmailAlreadyRegisteredException.class);
		verify(users, never()).save(any());
	}

	@Test
	void logsInWithValidCredentials() {
		var ash = User.register("ash", "ash@pallet.town", HASH, NOW);
		var token = new AccessToken("jwt", NOW.plusSeconds(7200));
		when(users.findByUsername("ash")).thenReturn(Optional.of(ash));
		when(hasher.matches(PASSWORD, HASH)).thenReturn(true);
		when(tokenIssuer.issueFor(ash)).thenReturn(token);

		assertThat(service.login(" Ash ", PASSWORD)).isEqualTo(token);
	}

	@Test
	void aWrongPasswordAndAnUnknownUserFailTheSameWay() {
		var ash = User.register("ash", "ash@pallet.town", HASH, NOW);
		when(users.findByUsername("ash")).thenReturn(Optional.of(ash));
		when(hasher.matches(PASSWORD, HASH)).thenReturn(false);
		when(users.findByUsername("gary")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login("ash", PASSWORD)).isInstanceOf(InvalidCredentialsException.class)
			.hasMessage("Invalid username or password");
		assertThatThrownBy(() -> service.login("gary", PASSWORD)).isInstanceOf(InvalidCredentialsException.class)
			.hasMessage("Invalid username or password");
		verify(tokenIssuer, never()).issueFor(any());
	}
}
