package com.poke.localpokemon.service;

import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonNotFoundException;
import com.poke.catalog.service.CatalogService;
import com.poke.localpokemon.domain.LocalPokemonAlreadyExistsException;
import com.poke.localpokemon.domain.LocalPokemonNotFoundException;
import com.poke.localpokemon.domain.LocalPokemonRepository;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.ProprietaryPatch;
import com.poke.localpokemon.domain.StaleVersionException;
import com.poke.shared.pagination.Page;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.IMPORTED_AT;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.LATER;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.importedPikachu;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuProprietary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPokemonServiceTest {

	@Mock
	LocalPokemonRepository repository;

	@Mock
	CatalogService catalogService;

	LocalPokemonService service;

	@BeforeEach
	void setUp() {
		service = new LocalPokemonService(repository, catalogService, Clock.fixed(LATER, ZoneOffset.UTC));
	}

	@Test
	void importCopiesThePokeApiDataWithoutProprietaryData() {
		when(catalogService.getDetail("Pikachu")).thenReturn(pikachuDetail());
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var imported = service.importPokemon("Pikachu");

		assertThat(imported.id()).isEqualTo(25);
		assertThat(imported.upstream().name()).isEqualTo("pikachu");
		assertThat(imported.upstream().spriteUrl()).isEqualTo("https://img/25.png");
		assertThat(imported.upstream().imageUrl()).isEqualTo("https://img/art/25.png");
		assertThat(imported.upstream().category()).isEqualTo("Mouse Pokemon");
		assertThat(imported.upstream().weightHectograms()).isEqualTo(60);
		assertThat(imported.upstream().types()).containsExactly("electric");
		assertThat(imported.upstream().abilities()).containsExactly("static", "lightning-rod");
		assertThat(imported.proprietary()).isEqualTo(ProprietaryData.NONE);
		assertThat(imported.version()).isZero();
		assertThat(imported.syncedAt()).isEqualTo(LATER);
	}

	@Test
	void importRejectsAPokemonThatIsAlreadyLocal() {
		when(catalogService.getDetail("25")).thenReturn(pikachuDetail());
		when(repository.existsById(25)).thenReturn(true);

		assertThatThrownBy(() -> service.importPokemon("25")).isInstanceOf(LocalPokemonAlreadyExistsException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void importPropagatesAnUnknownPokemon() {
		when(catalogService.getDetail("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		assertThatThrownBy(() -> service.importPokemon("missingno")).isInstanceOf(PokemonNotFoundException.class);
	}

	@Test
	void getReturnsTheLocalPokemonOrFailsWithNotFound() {
		when(repository.findById(25)).thenReturn(Optional.of(importedPikachu()));
		when(repository.findById(26)).thenReturn(Optional.empty());

		assertThat(service.get(25).id()).isEqualTo(25);
		assertThatThrownBy(() -> service.get(26)).isInstanceOf(LocalPokemonNotFoundException.class);
	}

	@Test
	void listDelegatesTheRequestedPage() {
		var request = new PageRequest(0, 20);
		var page = new Page<>(List.of(importedPikachu()), 0, 20, 1);
		when(repository.findPage(request)).thenReturn(page);

		assertThat(service.list(request)).isSameAs(page);
	}

	@Test
	void updateReplacesTheProprietaryDataWhenTheVersionMatches() {
		when(repository.findById(25)).thenReturn(Optional.of(importedPikachu()));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var updated = service.update(25, 0, pikachuProprietary());

		assertThat(updated.proprietary()).isEqualTo(pikachuProprietary());
		assertThat(updated.syncedAt()).isEqualTo(IMPORTED_AT);
		assertThat(updated.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void updateRejectsAStaleVersion() {
		when(repository.findById(25)).thenReturn(Optional.of(importedPikachu()));

		assertThatThrownBy(() -> service.update(25, 7, pikachuProprietary())).isInstanceOf(StaleVersionException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void updateFailsForAnUnknownLocalPokemon() {
		when(repository.findById(26)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(26, 0, pikachuProprietary()))
				.isInstanceOf(LocalPokemonNotFoundException.class);
	}

	@Test
	void patchChangesOnlyTheFieldsThatArePresent() {
		var annotated = importedPikachu().withProprietary(pikachuProprietary(), IMPORTED_AT);
		when(repository.findById(25)).thenReturn(Optional.of(annotated));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var patched = service.patch(25, 0, new ProprietaryPatch(null, "Johto", null, Set.of("electric"), null));

		assertThat(patched.proprietary().region()).isEqualTo("Johto");
		assertThat(patched.proprietary().tags()).containsExactly("electric");
		assertThat(patched.proprietary().localizedName()).isEqualTo("ピカチュウ");
		assertThat(patched.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void patchRejectsAStaleVersion() {
		when(repository.findById(25)).thenReturn(Optional.of(importedPikachu()));

		assertThatThrownBy(() -> service.patch(25, 1, new ProprietaryPatch(null, "Johto", null, null, null)))
				.isInstanceOf(StaleVersionException.class);
	}

	@Test
	void deleteRemovesAnExistingLocalPokemon() {
		when(repository.existsById(25)).thenReturn(true);

		service.delete(25);

		verify(repository).deleteById(25);
	}

	@Test
	void deleteFailsForAnUnknownLocalPokemon() {
		when(repository.existsById(26)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(26)).isInstanceOf(LocalPokemonNotFoundException.class);
		verify(repository, never()).deleteById(26);
	}

	private static PokemonDetail pikachuDetail() {
		return new PokemonDetail(25, "pikachu", "https://img/25.png", "https://img/art/25.png", "Mouse Pokemon", 60, 4,
				List.of("electric"), List.of(new Ability("static", false), new Ability("lightning-rod", true)),
				List.of(), "Possesses cheek sacs.", List.of());
	}
}
