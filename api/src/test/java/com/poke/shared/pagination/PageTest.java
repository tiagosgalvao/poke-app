package com.poke.shared.pagination;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

	@Test
	void derivesTotalPagesRoundingUp() {
		assertThat(new Page<>(List.of("a"), 0, 20, 1351).totalPages()).isEqualTo(68);
		assertThat(new Page<>(List.of("a"), 0, 20, 40).totalPages()).isEqualTo(2);
		assertThat(new Page<>(List.of(), 0, 20, 0).totalPages()).isZero();
	}

	@Test
	void contentIsAnImmutableCopy() {
		var source = new ArrayList<>(List.of("a", "b"));
		var page = new Page<>(source, 0, 20, 2);
		source.add("c");

		assertThat(page.content()).containsExactly("a", "b");
		assertThatThrownBy(() -> page.content().add("d")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void mapsContentKeepingPagination() {
		var page = new Page<>(List.of(1, 2), 1, 2, 10).map(n -> "#" + n);

		assertThat(page.content()).containsExactly("#1", "#2");
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(10);
	}

	@Test
	void rejectsInconsistentValues() {
		assertThatThrownBy(() -> new Page<>(null, 0, 20, 0)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new Page<>(List.of(), -1, 20, 0)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new Page<>(List.of(), 0, 0, 0)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new Page<>(List.of(), 0, 20, -1)).isInstanceOf(DomainValidationException.class);
	}
}
