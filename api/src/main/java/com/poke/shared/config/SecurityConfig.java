package com.poke.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration(proxyBeanMethods = false)
@Import(JwtConfig.class)
public class SecurityConfig {

	private static final String API = "/api/v1/**";
	private static final String AUTH_API = "/api/v1/auth/**";
	private static final String[] PUBLIC_INFRASTRUCTURE = {
			"/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error"};

	@Bean
	ProblemDetailSecurityHandler problemDetailSecurityHandler(JsonMapper jsonMapper) {
		return new ProblemDetailSecurityHandler(jsonMapper);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandler problemHandler) {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(PUBLIC_INFRASTRUCTURE).permitAll()
						.requestMatchers(GET, API).permitAll()
						.requestMatchers(POST, AUTH_API).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler))
				.build();
	}
}
