package com.poke.localpokemon.repository;

import com.poke.TestcontainersConfiguration;
import com.poke.localpokemon.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static com.poke.localpokemon.domain.LocalPokemonFixtures.IMPORTED_AT;
import static com.poke.localpokemon.domain.LocalPokemonFixtures.LATER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentUpdateTest {

	private static final int TEST_ID = 999;

	@Autowired
	LocalPokemonRepository repository;

	@Autowired
	PlatformTransactionManager transactionManager;

	@BeforeEach
	void storeATestPokemon() {
		repository.save(LocalPokemon.importFrom(TEST_ID, new UpstreamData("test-concurrency", null, null, null, 10, 5,
			List.of(), List.of()), IMPORTED_AT));
	}

	@AfterEach
	void cleanUp() {
		repository.deleteById(TEST_ID);
	}

	@Test
	void theSecondOfTwoConcurrentUpdatesIsRejectedAsStale() {
		var outer = new TransactionTemplate(transactionManager);
		var concurrent = new TransactionTemplate(transactionManager);
		concurrent.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);

		assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
			var readBeforeTheOtherWrite = repository.findById(TEST_ID).orElseThrow();
			concurrent.executeWithoutResult(other -> repository.save(
				repository.findById(TEST_ID).orElseThrow().withProprietary(withRegion("Kanto"), LATER)));

			repository.save(readBeforeTheOtherWrite.withProprietary(withRegion("Johto"), LATER));
		})).isInstanceOf(StaleVersionException.class).hasMessageContaining(String.valueOf(TEST_ID));

		var stored = repository.findById(TEST_ID).orElseThrow();
		assertThat(stored.proprietary().region()).isEqualTo("Kanto");
		assertThat(stored.version()).isEqualTo(1);
	}

	private static ProprietaryData withRegion(String region) {
		return new ProprietaryData(null, region, null, Set.of(), null);
	}
}
