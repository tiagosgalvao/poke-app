package com.poke.identity.service;

import com.poke.identity.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static com.poke.identity.domain.User.normalized;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordHasher hasher;
	private final TokenIssuer tokenIssuer;
	private final Clock clock;

	public AuthService(UserRepository users, PasswordHasher hasher, TokenIssuer tokenIssuer, Clock clock) {
		this.users = users;
		this.hasher = hasher;
		this.tokenIssuer = tokenIssuer;
		this.clock = clock;
	}

	@Transactional
	public User register(String username, String email, RawPassword password) {
		var normalizedUsername = normalized(username);
		var normalizedEmail = normalized(email);
		if (users.existsByUsername(normalizedUsername)) {
			throw new UsernameAlreadyTakenException(normalizedUsername);
		}
		if (users.existsByEmail(normalizedEmail)) {
			throw new EmailAlreadyRegisteredException();
		}
		return users.save(User.register(normalizedUsername, normalizedEmail, hasher.hash(password), clock.instant()));
	}

	@Transactional(readOnly = true)
	public AccessToken login(String username, RawPassword password) {
		return users.findByUsername(normalized(username))
			.filter(user -> hasher.matches(password, user.passwordHash()))
			.map(tokenIssuer::issueFor)
			.orElseThrow(InvalidCredentialsException::new);
	}
}
