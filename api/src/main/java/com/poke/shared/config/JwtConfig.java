package com.poke.shared.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {

	private static final String HMAC_SHA_256 = "HmacSHA256";

	@Bean
	public SecretKey jwtSigningKey(SecurityProperties properties) {
		return new SecretKeySpec(properties.jwtSecret().getBytes(UTF_8), HMAC_SHA_256);
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(HS256).build();
	}
}
