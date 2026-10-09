package com.poke.shared.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("poke.security")
public record SecurityProperties(
	@NotNull @Size(min = SecurityProperties.MIN_SECRET_LENGTH) String jwtSecret,
	@NotNull Duration tokenTtl) {

	public static final int MIN_SECRET_LENGTH = 32;
}
