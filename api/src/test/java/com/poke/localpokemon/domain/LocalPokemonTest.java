package com.poke.localpokemon.domain;

import com.poke.shared.exception.ConflictException;
import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.IMPORTED_AT;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.LATER;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.importedPikachu;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuProprietary;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.pikachuUpstream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonTest {

	@Test
	void importStartsWithoutProprietaryDataAtVersionZero() {
		var pikachu = importedPikachu();

		assertThat(pikachu.id()).isEqualTo(25);
		assertThat(pikachu.upstream()).isEqualTo(pikachuUpstream());
		assertThat(pikachu.proprietary()).isEqualTo(ProprietaryData.NONE);
		assertThat(pikachu.version()).isZero();
		assertThat(pikachu.syncedAt()).isEqualTo(IMPORTED_AT);
		assertThat(pikachu.updatedAt()).isEqualTo(IMPORTED_AT);
	}

	@Test
	void refreshingFromPokeApiNeverTouchesProprietaryData() {
		var annotated = importedPikachu().withProprietary(pikachuProprietary(), IMPORTED_AT);
		var evolvedStats = new UpstreamData("pikachu", null, null, "Mouse Pokemon", 61, 4, List.of("electric"), List.of("static"));

		var refreshed = annotated.refreshedWith(evolvedStats, LATER);

		assertThat(refreshed.upstream()).isEqualTo(evolvedStats);
		assertThat(refreshed.proprietary()).isEqualTo(pikachuProprietary());
		assertThat(refreshed.syncedAt()).isEqualTo(LATER);
		assertThat(refreshed.updatedAt()).isEqualTo(LATER);
		assertThat(refreshed.version()).isEqualTo(annotated.version());
	}

	@Test
	void editingProprietaryDataKeepsUpstreamDataAndSyncTime() {
		var edited = importedPikachu().withProprietary(pikachuProprietary(), LATER);

		assertThat(edited.proprietary()).isEqualTo(pikachuProprietary());
		assertThat(edited.upstream()).isEqualTo(pikachuUpstream());
		assertThat(edited.syncedAt()).isEqualTo(IMPORTED_AT);
		assertThat(edited.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void acceptsTheCurrentVersion() {
		assertThatNoException().isThrownBy(() -> importedPikachu().checkVersion(0));
	}

	@Test
	void rejectsAStaleVersionAsAConflict() {
		assertThatThrownBy(() -> importedPikachu().checkVersion(3))
				.isInstanceOf(StaleVersionException.class)
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("25");
	}

	@Test
	void rejectsInvalidState() {
		assertThatThrownBy(() -> LocalPokemon.importFrom(0, pikachuUpstream(), IMPORTED_AT))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> LocalPokemon.importFrom(25, null, IMPORTED_AT))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> LocalPokemon.importFrom(25, pikachuUpstream(), null))
				.isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new LocalPokemon(25, pikachuUpstream(), ProprietaryData.NONE, -1, IMPORTED_AT, IMPORTED_AT))
				.isInstanceOf(DomainValidationException.class);
	}

	@Test
	void notFoundAndDuplicateErrorsNameThePokemon() {
		assertThat(new LocalPokemonNotFoundException(25)).hasMessageContaining("25");
		assertThat(new LocalPokemonAlreadyExistsException(25))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("25");
	}
}
