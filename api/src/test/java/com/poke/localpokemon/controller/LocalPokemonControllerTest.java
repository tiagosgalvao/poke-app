package com.poke.localpokemon.controller;

import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.localpokemon.domain.*;
import com.poke.localpokemon.service.LocalPokemonService;
import com.poke.shared.config.SecurityConfig;
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

import static com.poke.localpokemon.domain.LocalPokemonFixtures.importedPikachu;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuProprietary;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LocalPokemonController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, LocalPokemonResponseMapperImpl.class})
class LocalPokemonControllerTest {

	private static final String LOCAL_POKEMON = "/api/v1/local-pokemon";
	private static final String PIKACHU = LOCAL_POKEMON + "/25";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	LocalPokemonService localPokemonService;

	@Test
	void mutationsWithoutATokenAreUnauthorized() throws Exception {
		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\"pikachu\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.instance").value(LOCAL_POKEMON));
		mvc.perform(put(PIKACHU).contentType(APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isUnauthorized());
		mvc.perform(patch(PIKACHU).contentType(APPLICATION_JSON).content("{\"version\":0}")).andExpect(status().isUnauthorized());
		mvc.perform(delete(PIKACHU)).andExpect(status().isUnauthorized());
		mvc.perform(post(LOCAL_POKEMON + "/sync").contentType(APPLICATION_JSON).content("{\"ids\":[1]}"))
			.andExpect(status().isUnauthorized());
		verifyNoInteractions(localPokemonService);
	}

	@Test
	void anInvalidTokenIsUnauthorized() throws Exception {
		mvc.perform(delete(PIKACHU).header("Authorization", "Bearer not-a-real-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listsLocalPokemonPageByPage() throws Exception {
		when(localPokemonService.list(new PageRequest(0, 20))).thenReturn(new Page<>(List.of(importedPikachu()), 0, 20, 1));

		mvc.perform(get(LOCAL_POKEMON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(25))
			.andExpect(jsonPath("$.content[0].name").value("pikachu"))
			.andExpect(jsonPath("$.content[0].weightKg").value(6.0))
			.andExpect(jsonPath("$.content[0].version").value(0));
	}

	@Test
	void returnsOneLocalPokemonWithItsProprietaryData() throws Exception {
		when(localPokemonService.get(25)).thenReturn(importedPikachu().withProprietary(pikachuProprietary(), importedPikachu().syncedAt()));

		mvc.perform(get(PIKACHU))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.spriteUrl").value("https://img/25.png"))
			.andExpect(jsonPath("$.types[0]").value("electric"))
			.andExpect(jsonPath("$.abilities[1]").value("lightning-rod"))
			.andExpect(jsonPath("$.localizedName").value("ピカチュウ"))
			.andExpect(jsonPath("$.region").value("Kanto"))
			.andExpect(jsonPath("$.tags[0]").value("mascot"))
			.andExpect(jsonPath("$.syncedAt").value("2026-10-08T10:00:00Z"));
	}

	@Test
	void unknownLocalPokemonIsA404() throws Exception {
		when(localPokemonService.get(26)).thenThrow(new LocalPokemonNotFoundException(26));

		mvc.perform(get(LOCAL_POKEMON + "/26"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void importsAPokemonFromPokeApi() throws Exception {
		when(localPokemonService.importPokemon("pikachu")).thenReturn(importedPikachu());

		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\"pikachu\"}").with(jwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(25));
	}

	@Test
	void importingTwiceIsAConflict() throws Exception {
		when(localPokemonService.importPokemon("pikachu")).thenThrow(new LocalPokemonAlreadyExistsException(25));

		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\"pikachu\"}").with(jwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Pokemon 25 is already in the local Pokedex"));
	}

	@Test
	void importingAnUnknownPokemonIsA404() throws Exception {
		when(localPokemonService.importPokemon("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\"missingno\"}").with(jwt()))
			.andExpect(status().isNotFound());
	}

	@Test
	void importRequiresAnIdOrName() throws Exception {
		mvc.perform(post(LOCAL_POKEMON).contentType(APPLICATION_JSON).content("{\"idOrName\":\" \"}").with(jwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("idOrName"));
		verifyNoInteractions(localPokemonService);
	}

	@Test
	void replacesTheProprietaryData() throws Exception {
		when(localPokemonService.update(25, 0, pikachuProprietary())).thenReturn(importedPikachu());

		mvc.perform(put(PIKACHU).contentType(APPLICATION_JSON).content("""
				{"version":0,"localizedName":"ピカチュウ","region":"Kanto","habitat":"forest",
				 "tags":["starter","mascot"],"notes":"Ash's partner"}""").with(jwt()))
			.andExpect(status().isOk());

		verify(localPokemonService).update(25, 0, pikachuProprietary());
	}

	@Test
	void rejectsInvalidProprietaryDataWithFieldErrors() throws Exception {
		mvc.perform(put(PIKACHU).contentType(APPLICATION_JSON).content("""
				{"localizedName":"  ","tags":["no spaces allowed"]}""").with(jwt()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Validation failed"))
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'version')]").exists())
			.andExpect(jsonPath("$.fieldErrors[?(@.field == 'localizedName')]").exists())
			.andExpect(jsonPath("$.fieldErrors[?(@.field =~ /tags.*/)]").exists());
		verifyNoInteractions(localPokemonService);
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mvc.perform(put(PIKACHU).contentType(APPLICATION_JSON).content("{\"version\":").with(jwt()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
	}

	@Test
	void aStaleVersionIsAConflict() throws Exception {
		when(localPokemonService.patch(25, 0, new ProprietaryPatch(null, "Johto", null, null, null)))
			.thenThrow(new StaleVersionException(25, 0, 2));

		mvc.perform(patch(PIKACHU).contentType(APPLICATION_JSON).content("{\"version\":0,\"region\":\"Johto\"}").with(jwt()))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Local Pokemon 25 was modified meanwhile (version 2, not 0). Reload it and try again."));
	}

	@Test
	void patchesOnlyTheFieldsThatArePresent() throws Exception {
		var expectedPatch = new ProprietaryPatch(null, "Johto", null, null, null);
		when(localPokemonService.patch(25, 3, expectedPatch)).thenReturn(importedPikachu());

		mvc.perform(patch(PIKACHU).contentType(APPLICATION_JSON).content("{\"version\":3,\"region\":\"Johto\"}").with(jwt()))
			.andExpect(status().isOk());

		verify(localPokemonService).patch(25, 3, expectedPatch);
	}

	@Test
	void deletesALocalPokemon() throws Exception {
		mvc.perform(delete(PIKACHU).with(jwt())).andExpect(status().isNoContent());

		verify(localPokemonService).delete(25);
	}

	@Test
	void syncsAListOfIds() throws Exception {
		when(localPokemonService.sync(new SyncBatch(List.of(1, 4, 7))))
			.thenReturn(new SyncSummary(List.of(1, 4), List.of(7), List.of()));

		mvc.perform(post(LOCAL_POKEMON + "/sync").contentType(APPLICATION_JSON).content("{\"ids\":[1,4,7]}").with(jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.created[1]").value(4))
			.andExpect(jsonPath("$.refreshed[0]").value(7))
			.andExpect(jsonPath("$.failed").isEmpty());
	}

	@Test
	void syncsARangeOfIds() throws Exception {
		when(localPokemonService.sync(SyncBatch.range(1, 3))).thenReturn(new SyncSummary(List.of(1, 2, 3), List.of(), List.of()));

		mvc.perform(post(LOCAL_POKEMON + "/sync").contentType(APPLICATION_JSON).content("{\"fromId\":1,\"toId\":3}").with(jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.created.length()").value(3));
	}

	@Test
	void aSyncRequestNeedsIdsOrARange() throws Exception {
		mvc.perform(post(LOCAL_POKEMON + "/sync").contentType(APPLICATION_JSON).content("{\"fromId\":1}").with(jwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("provide either ids or both fromId and toId"));
		verifyNoInteractions(localPokemonService);
	}

	@Test
	void rejectsAnOversizedSyncBatch() throws Exception {
		mvc.perform(post(LOCAL_POKEMON + "/sync").contentType(APPLICATION_JSON).content("{\"fromId\":1,\"toId\":500}").with(jwt()))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(localPokemonService);
	}
}
