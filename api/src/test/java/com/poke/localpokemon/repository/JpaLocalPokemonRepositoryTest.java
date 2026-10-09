package com.poke.localpokemon.repository;

import com.poke.TestcontainersConfiguration;
import com.poke.localpokemon.domain.LocalPokemon;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.UpstreamData;
import com.poke.localpokemon.entity.LocalPokemonEntityMapperImpl;
import com.poke.shared.pagination.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({TestcontainersConfiguration.class, JpaLocalPokemonRepository.class, LocalPokemonEntityMapperImpl.class})
class JpaLocalPokemonRepositoryTest {

	@Autowired
	JpaLocalPokemonRepository repository;

	@Autowired
	LocalPokemonJpaRepository jpa;

	@Autowired
	TestEntityManager entityManager;

	@BeforeEach
	void startFromAnEmptyPokedex() {
		jpa.deleteAll();
		entityManager.flush();
	}

	@Test
	void savesAndReloadsEveryField() {
		var annotated = importedPikachu().withProprietary(pikachuProprietary(), LATER);

		repository.save(annotated);
		entityManager.clear();
		var reloaded = repository.findById(25).orElseThrow();

		assertThat(reloaded.upstream()).isEqualTo(annotated.upstream());
		assertThat(reloaded.proprietary()).isEqualTo(pikachuProprietary());
		assertThat(reloaded.syncedAt()).isEqualTo(IMPORTED_AT);
		assertThat(reloaded.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void keepsTheUpstreamOrderOfTypesAndAbilities() {
		var charizard = LocalPokemon.importFrom(6, new UpstreamData("charizard", null, null, "Flame Pokemon", 905, 17,
			List.of("fire", "flying"), List.of("blaze", "solar-power")), IMPORTED_AT);

		repository.save(charizard);
		entityManager.clear();
		var reloaded = repository.findById(6).orElseThrow();

		assertThat(reloaded.upstream().types()).containsExactly("fire", "flying");
		assertThat(reloaded.upstream().abilities()).containsExactly("blaze", "solar-power");
	}

	@Test
	void aNewPokemonStartsAtVersionZeroAndEveryUpdateIncrementsIt() {
		var saved = repository.save(importedPikachu());
		entityManager.clear();

		var updated = repository.save(saved.withProprietary(pikachuProprietary(), LATER));

		assertThat(saved.version()).isZero();
		assertThat(updated.version()).isEqualTo(1);
	}

	@Test
	void replacesTagsOnUpdate() {
		repository.save(importedPikachu().withProprietary(pikachuProprietary(), LATER));
		entityManager.clear();
		var current = repository.findById(25).orElseThrow();

		repository.save(current.withProprietary(new ProprietaryData(null, null, null, Set.of("electric"), null), LATER));
		entityManager.clear();

		assertThat(repository.findById(25).orElseThrow().proprietary().tags()).containsExactly("electric");
	}

	@Test
	void reportsExistenceAndDeletes() {
		repository.save(importedPikachu());

		assertThat(repository.existsById(25)).isTrue();
		assertThat(repository.existsById(26)).isFalse();

		repository.deleteById(25);
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findById(25)).isEmpty();
	}

	@Test
	void pagesAreOrderedByNationalId() {
		for (var id : List.of(7, 1, 4)) {
			repository.save(LocalPokemon.importFrom(id, new UpstreamData("pokemon-" + id, null, null, null, 10, 5,
				List.of(), List.of()), IMPORTED_AT));
		}
		entityManager.clear();

		var page = repository.findPage(new PageRequest(0, 2));

		assertThat(page.content()).extracting(LocalPokemon::id).containsExactly(1, 4);
		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
	}
}
