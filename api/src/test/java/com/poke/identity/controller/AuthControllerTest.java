package com.poke.identity.controller;

import com.poke.identity.domain.*;
import com.poke.identity.service.AuthService;
import com.poke.shared.config.SecurityConfig;
import com.poke.shared.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, AuthResponseMapperImpl.class})
class AuthControllerTest {

	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
	private static final String ASH_REGISTRATION = """
		{"username":"ash","email":"ash@pallet.town","password":"Pikachu123!"}""";
	private static final String ASH_LOGIN = """
		{"username":"ash","password":"Pikachu123!"}""";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	AuthService authService;

	@Test
	void registersAUserWithoutExposingThePasswordHash() throws Exception {
		var ash = User.register("ash", "ash@pallet.town", "$2a$10$secret-hash", NOW);
		when(authService.register("ash", "ash@pallet.town", new RawPassword("Pikachu123!"))).thenReturn(ash);

		mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(ASH_REGISTRATION))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(ash.id().toString()))
			.andExpect(jsonPath("$.username").value("ash"))
			.andExpect(jsonPath("$.email").value("ash@pallet.town"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void aTakenUsernameOrEmailIsAConflict() throws Exception {
		when(authService.register("ash", "ash@pallet.town", new RawPassword("Pikachu123!")))
			.thenThrow(new UsernameAlreadyTakenException("ash"))
			.thenThrow(new EmailAlreadyRegisteredException());

		mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(ASH_REGISTRATION))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Username 'ash' is already taken"));
		mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content(ASH_REGISTRATION))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsAnInvalidRegistrationWithFieldErrors() throws Exception {
		mvc.perform(post(REGISTER).contentType(APPLICATION_JSON).content("""
				{"username":"a b","email":"not-an-email","password":"short"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'username')]").exists())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
		verifyNoInteractions(authService);
	}

	@Test
	void logsInAndReturnsABearerToken() throws Exception {
		var expiresAt = NOW.plusSeconds(7200);
		when(authService.login("ash", new RawPassword("Pikachu123!"))).thenReturn(new AccessToken("signed.jwt.value", expiresAt));

		mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content(ASH_LOGIN))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("signed.jwt.value"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresAt").value("2026-10-08T12:00:00Z"));
	}

	@Test
	void badCredentialsAreUnauthorized() throws Exception {
		when(authService.login("ash", new RawPassword("Pikachu123!"))).thenThrow(new InvalidCredentialsException());

		mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content(ASH_LOGIN))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Invalid username or password"));
	}

	@Test
	void loginRequiresBothFields() throws Exception {
		mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content("{\"username\":\"ash\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
		verifyNoInteractions(authService);
	}
}
