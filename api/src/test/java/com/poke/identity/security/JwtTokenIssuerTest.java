package com.poke.identity.security;

import com.poke.identity.domain.User;
import com.poke.shared.config.JwtConfig;
import com.poke.shared.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenIssuerTest {

	private static final SecurityProperties PROPERTIES =
		new SecurityProperties("a-test-secret-that-is-at-least-32-bytes-long", Duration.ofHours(2));
	private static final User ASH = User.register("ash", "ash@pallet.town", "hash", Instant.parse("2026-10-08T10:00:00Z"));

	private final JwtConfig jwtConfig = new JwtConfig();
	private final SecretKey key = jwtConfig.jwtSigningKey(PROPERTIES);

	@Test
	void issuesASignedTokenThatTheDecoderAccepts() {
		var now = Instant.now();
		var issuer = new JwtTokenIssuer(jwtConfig.jwtEncoder(key), PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));

		var token = issuer.issueFor(ASH);
		var decoded = jwtConfig.jwtDecoder(key).decode(token.value());

		assertThat(decoded.getSubject()).isEqualTo("ash");
		assertThat(decoded.getClaimAsString(JwtTokenIssuer.USER_ID_CLAIM)).isEqualTo(ASH.id().toString());
		assertThat(token.expiresAt()).isEqualTo(now.truncatedTo(SECONDS).plus(PROPERTIES.tokenTtl()));
		assertThat(decoded.getExpiresAt()).isEqualTo(token.expiresAt());
	}

	@Test
	void expiredTokensAreRejected() {
		var issuedLongAgo = new JwtTokenIssuer(jwtConfig.jwtEncoder(key), PROPERTIES,
			Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC));

		var token = issuedLongAgo.issueFor(ASH);

		assertThatThrownBy(() -> jwtConfig.jwtDecoder(key).decode(token.value())).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void tokensSignedWithAnotherSecretAreRejected() {
		var otherKey = jwtConfig.jwtSigningKey(new SecurityProperties("another-secret-that-is-also-32-bytes-long", Duration.ofHours(2)));
		var forged = new JwtTokenIssuer(jwtConfig.jwtEncoder(otherKey), PROPERTIES, Clock.systemUTC()).issueFor(ASH);

		assertThatThrownBy(() -> jwtConfig.jwtDecoder(key).decode(forged.value()))
			.isInstanceOf(BadJwtException.class);
	}
}
