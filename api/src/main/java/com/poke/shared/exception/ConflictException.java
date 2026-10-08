package com.poke.shared.exception;

public abstract class ConflictException extends DomainException {

	protected ConflictException(String message) {
		super(message);
	}
}
