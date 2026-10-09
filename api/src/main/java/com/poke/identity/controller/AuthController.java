package com.poke.identity.controller;

import com.poke.identity.controller.AuthRequests.LoginRequest;
import com.poke.identity.controller.AuthRequests.RegisterRequest;
import com.poke.identity.controller.AuthResponses.TokenResponse;
import com.poke.identity.controller.AuthResponses.UserResponse;
import com.poke.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(authService.register(request.username(), request.email(), request.rawPassword()));
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.from(authService.login(request.username(), request.rawPassword()));
	}
}
