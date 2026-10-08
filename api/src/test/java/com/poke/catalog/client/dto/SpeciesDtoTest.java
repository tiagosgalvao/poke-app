package com.poke.catalog.client.dto;

import com.poke.catalog.client.dto.SpeciesDto.FlavorText;
import com.poke.catalog.client.dto.SpeciesDto.Genus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpeciesDtoTest {

	private static final NamedResource ENGLISH = new NamedResource("en", null);
	private static final NamedResource JAPANESE = new NamedResource("ja", null);

	@Test
	void keepsOnlyTheGenusAndLatestFlavorTextOfTheRequestedLanguage() {
		var species = new SpeciesDto(25, "pikachu",
				List.of(new Genus("ねずみポケモン", JAPANESE), new Genus("Mouse Pokemon", ENGLISH)),
				List.of(new FlavorText("old", ENGLISH, null), new FlavorText("日本語", JAPANESE, null),
						new FlavorText("latest", ENGLISH, null)),
				new SpeciesDto.ApiResource("https://pokeapi.co/api/v2/evolution-chain/10/"));

		var trimmed = species.keepingOnlyLatestTextIn("en");

		assertThat(trimmed.genera()).extracting(Genus::genus).containsExactly("Mouse Pokemon");
		assertThat(trimmed.flavorTextEntries()).extracting(FlavorText::text).containsExactly("latest");
		assertThat(trimmed.evolutionChain()).isEqualTo(species.evolutionChain());
	}

	@Test
	void missingTextsBecomeEmptyLists() {
		var trimmed = new SpeciesDto(1, "x", null, null, null).keepingOnlyLatestTextIn("en");

		assertThat(trimmed.genera()).isEmpty();
		assertThat(trimmed.flavorTextEntries()).isEmpty();
	}
}
