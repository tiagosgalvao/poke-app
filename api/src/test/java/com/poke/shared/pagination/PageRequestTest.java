package com.poke.shared.pagination;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static com.poke.shared.pagination.PageRequest.MAX_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

	@Test
	void computesOffsetFromPageAndSize() {
		assertThat(new PageRequest(0, 20).offset()).isZero();
		assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
	}

	@Test
	void acceptsBoundaryValues() {
		assertThat(new PageRequest(0, 1).size()).isEqualTo(1);
		assertThat(new PageRequest(0, MAX_SIZE).size()).isEqualTo(50);
	}

	@Test
	void rejectsNegativePage() {
		assertThatThrownBy(() -> new PageRequest(-1, 20))
				.isInstanceOf(DomainValidationException.class)
				.hasMessageContaining("page");
	}

	@Test
	void rejectsSizeOutOfRange() {
		assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(DomainValidationException.class);
		assertThatThrownBy(() -> new PageRequest(0, 51))
				.isInstanceOf(DomainValidationException.class)
				.hasMessageContaining("size");
	}
}
