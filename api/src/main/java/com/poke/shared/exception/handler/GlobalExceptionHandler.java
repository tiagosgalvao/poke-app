package com.poke.shared.exception.handler;

import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import com.poke.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	public static final String CATALOG_UNAVAILABLE = "The Pokemon catalog is temporarily unavailable. Please try again later.";
	public static final String UNEXPECTED_ERROR = "An unexpected error occurred.";

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(DomainValidationException.class)
	ProblemDetail handleValidation(DomainValidationException exception) {
		return ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException exception) {
		return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(ExternalServiceUnavailableException.class)
	ProblemDetail handleUpstreamFailure(ExternalServiceUnavailableException exception) {
		LOGGER.warn("Upstream service failure: {}", exception.getMessage(), exception);
		return ProblemDetail.forStatusAndDetail(SERVICE_UNAVAILABLE, CATALOG_UNAVAILABLE);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		LOGGER.error("Unexpected error", exception);
		return ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
	}
}
