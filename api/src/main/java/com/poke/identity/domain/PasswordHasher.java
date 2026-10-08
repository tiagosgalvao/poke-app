package com.poke.identity.domain;

public interface PasswordHasher {

	String hash(RawPassword password);

	boolean matches(RawPassword password, String hash);
}
