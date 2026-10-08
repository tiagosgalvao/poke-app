package com.poke.shared.exception;

public class ExternalServiceUnavailableException extends DomainException {

	public ExternalServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
