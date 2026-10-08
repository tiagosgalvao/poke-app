package com.poke.shared.config;

import com.poke.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static com.poke.shared.config.OpenApiConfig.BEARER_AUTH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiConfigTest {

	@Autowired
	MockMvc mvc;

	@Test
	void documentsAJwtBearerScheme() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_AUTH + ".type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_AUTH + ".scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes." + BEARER_AUTH + ".bearerFormat").value("JWT"));
	}

	@Test
	void marksOnlyProtectedOperationsAsSecured() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(jsonPath("$.paths['/api/v1/local-pokemon/{id}'].put.security[0]." + BEARER_AUTH).exists())
				.andExpect(jsonPath("$.paths['/api/v1/local-pokemon/sync'].post.security[0]." + BEARER_AUTH).exists())
				.andExpect(jsonPath("$.paths['/api/v1/local-pokemon/{id}'].get.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/pokemon'].get.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
	}
}
