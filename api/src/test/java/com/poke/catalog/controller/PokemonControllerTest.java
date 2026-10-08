package com.poke.catalog.controller;

import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.EvolutionStage;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.catalog.domain.Stat;
import com.poke.catalog.service.CatalogService;
import com.poke.shared.config.SecurityConfig;
import com.poke.shared.exception.DomainValidationException;
import com.poke.shared.exception.ExternalServiceUnavailableException;
import com.poke.shared.exception.handler.GlobalExceptionHandler;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.poke.shared.exception.handler.GlobalExceptionHandler.CATALOG_UNAVAILABLE;
import static com.poke.shared.exception.handler.GlobalExceptionHandler.UNEXPECTED_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PokemonControllerTest {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	CatalogService catalogService;

	@Test
	void browseReturnsAPageOfSummariesWithMetricUnits() throws Exception {
		var pikachu = new PokemonSummary(25, "pikachu", "https://img/25.png", "Mouse Pokemon", 60, 4,
				List.of("electric"), List.of(new Ability("static", false), new Ability("lightning-rod", true)));
		when(catalogService.browse(new PageRequest(1, 2))).thenReturn(new Page<>(List.of(pikachu), 1, 2, 1351));

		mvc.perform(get("/api/v1/pokemon").param("page", "1").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(1351))
				.andExpect(jsonPath("$.totalPages").value(676))
				.andExpect(jsonPath("$.content[0].name").value("pikachu"))
				.andExpect(jsonPath("$.content[0].spriteUrl").value("https://img/25.png"))
				.andExpect(jsonPath("$.content[0].category").value("Mouse Pokemon"))
				.andExpect(jsonPath("$.content[0].weightKg").value(6.0))
				.andExpect(jsonPath("$.content[0].heightM").value(0.4))
				.andExpect(jsonPath("$.content[0].abilities[1].name").value("lightning-rod"))
				.andExpect(jsonPath("$.content[0].abilities[1].hidden").value(true));
	}

	@Test
	void browseDefaultsToTheFirstPageOfTwenty() throws Exception {
		when(catalogService.browse(new PageRequest(0, 20))).thenReturn(new Page<>(List.of(), 0, 20, 0));

		mvc.perform(get("/api/v1/pokemon")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20));
	}

	@Test
	void browseRejectsAnOversizedPageAsProblemDetail() throws Exception {
		mvc.perform(get("/api/v1/pokemon").param("size", "500"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("size must be between 1 and 50"));
	}

	@Test
	void browseRejectsNonNumericParameters() throws Exception {
		mvc.perform(get("/api/v1/pokemon").param("page", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void detailReturnsStatsDescriptionAndEvolution() throws Exception {
		var pikachu = new PokemonDetail(25, "pikachu", "https://img/25.png", "https://img/art/25.png", "Mouse Pokemon", 60, 4,
				List.of("electric"), List.of(new Ability("static", false)), List.of(new Stat("speed", 90)),
				"Possesses cheek sacs.",
				List.of(new EvolutionStage(0, 172, "pichu", null, null, "https://img/172.png"),
						new EvolutionStage(1, 25, "pikachu", 172, "level-up", "https://img/25.png")));
		when(catalogService.getDetail("pikachu")).thenReturn(pikachu);

		mvc.perform(get("/api/v1/pokemon/pikachu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spriteUrl").value("https://img/25.png"))
				.andExpect(jsonPath("$.imageUrl").value("https://img/art/25.png"))
				.andExpect(jsonPath("$.stats[0].name").value("speed"))
				.andExpect(jsonPath("$.stats[0].value").value(90))
				.andExpect(jsonPath("$.description").value("Possesses cheek sacs."))
				.andExpect(jsonPath("$.evolution[1].evolvesFromId").value(172))
				.andExpect(jsonPath("$.evolution[1].trigger").value("level-up"));
	}

	@Test
	void unknownPokemonIsA404ProblemDetail() throws Exception {
		when(catalogService.getDetail("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		mvc.perform(get("/api/v1/pokemon/missingno"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Pokemon 'missingno' was not found"))
				.andExpect(jsonPath("$.instance").value("/api/v1/pokemon/missingno"));
	}

	@Test
	void malformedKeyIsA400() throws Exception {
		when(catalogService.getDetail(any())).thenThrow(new DomainValidationException("bad key"));

		mvc.perform(get("/api/v1/pokemon/bad!key")).andExpect(status().isBadRequest());
	}

	@Test
	void upstreamOutageIsA503WithoutInternalDetails() throws Exception {
		when(catalogService.getDetail("25"))
				.thenThrow(new ExternalServiceUnavailableException("PokeAPI request failed: /pokemon/{key}/", null));

		mvc.perform(get("/api/v1/pokemon/25"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.detail").value(CATALOG_UNAVAILABLE));
	}

	@Test
	void unexpectedErrorsAreA500WithAGenericMessage() throws Exception {
		when(catalogService.getDetail("25")).thenThrow(new IllegalStateException("database password is hunter2"));

		mvc.perform(get("/api/v1/pokemon/25"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value(UNEXPECTED_ERROR));
	}
}
