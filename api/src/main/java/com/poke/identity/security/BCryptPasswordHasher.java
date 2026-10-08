package com.poke.identity.security;

import com.poke.identity.domain.PasswordHasher;
import com.poke.identity.domain.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hash(RawPassword password) {
		return encoder.encode(password.value());
	}

	@Override
	public boolean matches(RawPassword password, String hash) {
		return encoder.matches(password.value(), hash);
	}
}
