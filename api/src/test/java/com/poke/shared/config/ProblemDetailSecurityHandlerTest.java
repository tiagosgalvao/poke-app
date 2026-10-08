package com.poke.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

import static com.poke.shared.config.ProblemDetailSecurityHandler.ACCESS_DENIED;
import static com.poke.shared.config.ProblemDetailSecurityHandler.AUTHENTICATION_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

class ProblemDetailSecurityHandlerTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final ProblemDetailSecurityHandler handler = new ProblemDetailSecurityHandler(jsonMapper);

	@Test
	void writesA401ProblemDetailWhenAuthenticationIsMissing() throws Exception {
		var response = new MockHttpServletResponse();

		handler.commence(request(), response, new InsufficientAuthenticationException("no token"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).isEqualTo(APPLICATION_PROBLEM_JSON_VALUE);
		var body = jsonMapper.readTree(response.getContentAsString());
		assertThat(body.get("detail").asString()).isEqualTo(AUTHENTICATION_REQUIRED);
		assertThat(body.get("instance").asString()).isEqualTo("/api/v1/local-pokemon/25");
	}

	@Test
	void writesA403ProblemDetailWhenAccessIsDenied() throws Exception {
		var response = new MockHttpServletResponse();

		handler.handle(request(), response, new AccessDeniedException("denied"));

		assertThat(response.getStatus()).isEqualTo(403);
		var body = jsonMapper.readTree(response.getContentAsString());
		assertThat(body.get("title").asString()).isEqualTo("Forbidden");
		assertThat(body.get("detail").asString()).isEqualTo(ACCESS_DENIED);
	}

	private static MockHttpServletRequest request() {
		var request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/local-pokemon/25");
		return request;
	}
}
