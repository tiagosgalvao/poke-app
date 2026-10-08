package com.poke.localpokemon.domain;

import com.poke.shared.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.poke.localpokemon.domain.ProprietaryData.MAX_TAGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProprietaryDataTest {

	@Test
	void noneHasNoValuesAndNoTags() {
		assertThat(ProprietaryData.NONE.localizedName()).isNull();
		assertThat(ProprietaryData.NONE.region()).isNull();
		assertThat(ProprietaryData.NONE.habitat()).isNull();
		assertThat(ProprietaryData.NONE.notes()).isNull();
		assertThat(ProprietaryData.NONE.tags()).isEmpty();
	}

	@Test
	void normalizesTagsToTrimmedLowercaseInAlphabeticalOrder() {
		var data = new ProprietaryData(null, null, null, Set.of(" Starter ", "LEGENDARY", "starter"), null);

		assertThat(data.tags()).containsExactly("legendary", "starter");
	}

	@Test
	void tagsAreAnImmutableCopyAndNullBecomesEmpty() {
		var tags = new HashSet<>(Set.of("starter"));
		var data = new ProprietaryData(null, null, null, tags, null);
		tags.add("legendary");

		assertThat(data.tags()).containsExactly("starter");
		assertThatThrownBy(() -> data.tags().add("x")).isInstanceOf(UnsupportedOperationException.class);
		assertThat(new ProprietaryData(null, null, null, null, null).tags()).isEmpty();
	}

	@Test
	void acceptsUpToTheMaximumNumberOfTags() {
		assertThat(new ProprietaryData(null, null, null, tags(MAX_TAGS), null).tags()).hasSize(MAX_TAGS);
	}

	@Test
	void rejectsMoreThanTheMaximumNumberOfTags() {
		assertThatThrownBy(() -> new ProprietaryData(null, null, null, tags(MAX_TAGS + 1), null))
				.isInstanceOf(DomainValidationException.class)
				.hasMessageContaining("at most " + MAX_TAGS);
	}

	@Test
	void rejectsBlankTags() {
		assertThatThrownBy(() -> new ProprietaryData(null, null, null, Set.of("  "), null))
				.isInstanceOf(DomainValidationException.class);
	}

	private static Set<String> tags(int count) {
		return IntStream.rangeClosed(1, count).mapToObj(n -> "tag-" + n).collect(Collectors.toSet());
	}
}
