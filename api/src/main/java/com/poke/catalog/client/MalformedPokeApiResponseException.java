package com.poke.catalog.client;

import com.poke.shared.exception.ExternalServiceUnavailableException;

public class MalformedPokeApiResponseException extends ExternalServiceUnavailableException {

	public MalformedPokeApiResponseException(String message) {
		super(message, null);
	}

	public MalformedPokeApiResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
