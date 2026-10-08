package com.poke.localpokemon.repository;

import com.poke.TestcontainersConfiguration;
import com.poke.localpokemon.domain.LocalPokemonRepository;
import com.poke.localpokemon.domain.ProprietaryData;
import com.poke.localpokemon.domain.StaleVersionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.LATER;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.importedPikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentUpdateTest {

	@Autowired
	LocalPokemonRepository repository;

	@Autowired
	LocalPokemonJpaRepository jpa;

	@Autowired
	PlatformTransactionManager transactionManager;

	@BeforeEach
	void storePikachu() {
		jpa.deleteAll();
		repository.save(importedPikachu());
	}

	@AfterEach
	void cleanUp() {
		jpa.deleteAll();
	}

	@Test
	void theSecondOfTwoConcurrentUpdatesIsRejectedAsStale() {
		var outer = new TransactionTemplate(transactionManager);
		var concurrent = new TransactionTemplate(transactionManager);
		concurrent.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);

		assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
			var readBeforeTheOtherWrite = repository.findById(25).orElseThrow();
			concurrent.executeWithoutResult(other -> repository.save(
					repository.findById(25).orElseThrow().withProprietary(withRegion("Kanto"), LATER)));

			repository.save(readBeforeTheOtherWrite.withProprietary(withRegion("Johto"), LATER));
		})).isInstanceOf(StaleVersionException.class).hasMessageContaining("25");

		var stored = repository.findById(25).orElseThrow();
		assertThat(stored.proprietary().region()).isEqualTo("Kanto");
		assertThat(stored.version()).isEqualTo(1);
	}

	private static ProprietaryData withRegion(String region) {
		return new ProprietaryData(null, region, null, Set.of(), null);
	}
}
