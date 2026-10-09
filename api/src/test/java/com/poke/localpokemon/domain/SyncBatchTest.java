package com.poke.localpokemon.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static com.poke.localpokemon.domain.SyncBatch.MAX_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncBatchTest {

	@Test
	void keepsTheRequestedOrderAndDropsDuplicates() {
		assertThat(new SyncBatch(List.of(4, 1, 4, 7)).ids()).containsExactly(4, 1, 7);
	}

	@Test
	void buildsAnInclusiveRange() {
		assertThat(SyncBatch.range(1, 3).ids()).containsExactly(1, 2, 3);
		assertThat(SyncBatch.range(25, 25).ids()).containsExactly(25);
	}

	@Test
	void acceptsUpToTheMaximumBatchSize() {
		assertThat(SyncBatch.range(1, MAX_IDS).ids()).hasSize(MAX_IDS);
	}

	@Test
	void rejectsAnEmptyOrOversizedBatch() {
		assertThatThrownBy(() -> new SyncBatch(List.of())).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new SyncBatch(null)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new SyncBatch(IntStream.rangeClosed(1, MAX_IDS + 1).boxed().toList()))
			.isInstanceOf(DomainValidationException.class)
			.hasMessageContaining(String.valueOf(MAX_IDS));
	}

	@Test
	void rejectsNonPositiveIds() {
		assertThatThrownBy(() -> new SyncBatch(List.of(1, 0))).isInstanceOf(DomainValidationException.class);
	}

	@Test
	void rejectsAnInvertedOrOversizedRange() {
		assertThatThrownBy(() -> SyncBatch.range(5, 1)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> SyncBatch.range(1, MAX_IDS + 1)).isInstanceOf(DomainValidationException.class);
	}
}
