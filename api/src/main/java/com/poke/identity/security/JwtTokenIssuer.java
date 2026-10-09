package com.poke.identity.security;

import com.poke.identity.domain.AccessToken;
import com.poke.identity.domain.TokenIssuer;
import com.poke.identity.domain.User;
import com.poke.shared.config.SecurityProperties;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;

@Component
public class JwtTokenIssuer implements TokenIssuer {

	public static final String ISSUER = "poke-app";
	public static final String USER_ID_CLAIM = "uid";

	private final JwtEncoder encoder;
	private final Duration tokenTtl;
	private final Clock clock;

	public JwtTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
		this.encoder = encoder;
		this.tokenTtl = properties.tokenTtl();
		this.clock = clock;
	}

	@Override
	public AccessToken issueFor(User user) {
		var issuedAt = clock.instant().truncatedTo(SECONDS);
		var expiresAt = issuedAt.plus(tokenTtl);
		var claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(user.username())
			.claim(USER_ID_CLAIM, user.id().toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();
		var token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(HS256).build(), claims));
		return new AccessToken(token.getTokenValue(), expiresAt);
	}
}
