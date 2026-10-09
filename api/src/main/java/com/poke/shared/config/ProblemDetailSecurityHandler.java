package com.poke.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String AUTHENTICATION_REQUIRED = "A valid bearer token is required to access this resource.";
	static final String ACCESS_DENIED = "You are not allowed to access this resource.";

	private final JsonMapper jsonMapper;

	public ProblemDetailSecurityHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
		write(request, response, UNAUTHORIZED, AUTHENTICATION_REQUIRED);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
		throws IOException {
		write(request, response, FORBIDDEN, ACCESS_DENIED);
	}

	private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		var problem = new LinkedHashMap<String, Object>();
		problem.put("title", status.getReasonPhrase());
		problem.put("status", status.value());
		problem.put("detail", detail);
		problem.put("instance", request.getRequestURI());
		response.setStatus(status.value());
		response.setContentType(APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}
}
