package com.poke.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	private static final String TITLE = "Poke App API";
	private static final String DESCRIPTION = "Pokemon catalog (PokeAPI), local Pokedex and authentication";
	private static final String VERSION = "v1";
	private static final String BEARER = "bearer";
	private static final String JWT = "JWT";
	private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

	@Bean
	OpenAPI pokeAppOpenApi() {
		return new OpenAPI()
			.info(new Info().title(TITLE).description(DESCRIPTION).version(VERSION))
			.components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme(BEARER)
				.bearerFormat(JWT)));
	}

	@Bean
	OpenApiCustomizer protectedOperationsRequireBearerAuth() {
		return openApi -> openApi.getPaths().forEach((path, item) -> {
			if (!path.startsWith(AUTH_PATH_PREFIX)) {
				requireBearerAuthOnMutations(item);
			}
		});
	}

	private static void requireBearerAuthOnMutations(PathItem item) {
		item.readOperationsMap().forEach((method, operation) -> {
			if (method != GET) {
				operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
			}
		});
	}
}
