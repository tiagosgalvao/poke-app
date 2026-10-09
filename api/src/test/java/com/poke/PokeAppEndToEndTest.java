package com.poke;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.poke.catalog.client.PokeApiFixtures.stubFixture;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PokeAppEndToEndTest {

	private static final String REGISTER = "/api/v1/auth/register";
	private static final String LOGIN = "/api/v1/auth/login";
	private static final String LOCAL_POKEMON = "/api/v1/local-pokemon";
	private static final String DEOXYS_ATTACK = LOCAL_POKEMON + "/10001";
	private static final String TRAINER = "e2e-trainer";
	private static final String PASSWORD = "Trainer123!";

	@RegisterExtension
	static WireMockExtension pokeApi = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

	@DynamicPropertySource
	static void pointToWireMock(DynamicPropertyRegistry registry) {
		registry.add("poke.pokeapi.base-url", pokeApi::baseUrl);
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	JsonMapper jsonMapper;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void removeTestData() {
		jdbc.update("delete from local_pokemon where id = 10001");
		jdbc.update("delete from users where username = ?", TRAINER);
	}

	@Test
	void aTrainerRegistersImportsEditsSyncsAndDeletesALocalPokemon() throws Exception {
		stubFixture(pokeApi, "/pokemon/10001/", "pokemon-10001.json");
		stubFixture(pokeApi, "/pokemon-species/386/", "pokemon-species-386.json");

		mvc.perform(post(REGISTER).contentType(APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"email\":\"e2e@poke.app\",\"password\":\"%s\"}".formatted(TRAINER, PASSWORD)))
			.andExpect(status().isCreated());
		var token = loginToken();

		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\"10001\"}"))
			.andExpect(status().isUnauthorized());

		mvc.perform(authorized(post(LOCAL_POKEMON), token).content("{\"idOrName\":\"10001\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("deoxys-attack"))
			.andExpect(jsonPath("$.category").value("DNA Pokémon"))
			.andExpect(jsonPath("$.version").value(0));
		mvc.perform(authorized(post(LOCAL_POKEMON), token).content("{\"idOrName\":\"10001\"}"))
			.andExpect(status().isConflict());

		mvc.perform(authorized(patch(DEOXYS_ATTACK), token).content("{\"version\":0,\"region\":\"Hoenn\",\"tags\":[\"mythical\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(1));
		mvc.perform(authorized(patch(DEOXYS_ATTACK), token).content("{\"version\":0,\"region\":\"Kanto\"}"))
			.andExpect(status().isConflict());
		mvc.perform(authorized(put(DEOXYS_ATTACK), token).content("{\"version\":1,\"localizedName\":\"  \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("localizedName"));

		mvc.perform(authorized(post(LOCAL_POKEMON + "/sync"), token).content("{\"ids\":[10001,99999]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.refreshed[0]").value(10001))
			.andExpect(jsonPath("$.failed[0]").value(99999));
		mvc.perform(get(DEOXYS_ATTACK))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.region").value("Hoenn"))
			.andExpect(jsonPath("$.tags[0]").value("mythical"));

		mvc.perform(authorized(delete(DEOXYS_ATTACK), token)).andExpect(status().isNoContent());
		mvc.perform(get(DEOXYS_ATTACK)).andExpect(status().isNotFound());
	}

	@Test
	void theCatalogIsPublicAndUnknownPokemonAreNotFound() throws Exception {
		stubFixture(pokeApi, "/pokemon/10001/", "pokemon-10001.json");
		stubFixture(pokeApi, "/pokemon-species/386/", "pokemon-species-386.json");

		mvc.perform(get("/api/v1/pokemon/10001")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("deoxys-attack"));
		mvc.perform(get("/api/v1/pokemon/missingno")).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/pokemon").param("size", "500")).andExpect(status().isBadRequest());
	}

	@Test
	void wrongCredentialsAreUnauthorized() throws Exception {
		mvc.perform(post(LOGIN).contentType(APPLICATION_JSON).content("{\"username\":\"ash\",\"password\":\"NotHisPassword1\"}"))
			.andExpect(status().isUnauthorized());
	}

	private String loginToken() throws Exception {
		var body = mvc.perform(post(LOGIN).contentType(APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(TRAINER, PASSWORD)))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(body).get("accessToken").asString();
	}

	private static MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
		return request.header(AUTHORIZATION, "Bearer " + token).contentType(APPLICATION_JSON);
	}
}
