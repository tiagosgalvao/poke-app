package com.poke.catalog.client;

import com.poke.catalog.client.dto.PokemonDto;
import com.poke.catalog.client.dto.SpeciesDto;
import com.poke.catalog.domain.Ability;
import com.poke.catalog.domain.EvolutionStage;
import com.poke.catalog.domain.PokemonDetail;
import com.poke.catalog.domain.PokemonSummary;
import com.poke.catalog.domain.Stat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.poke.catalog.client.dto.NamedResource.isNamed;
import static com.poke.catalog.client.dto.NullSafeLists.orEmpty;
import static com.poke.catalog.client.enums.PokeApiLanguage.ENGLISH;

final class PokeApiMapper {

	private PokeApiMapper() {
	}

	static PokemonSummary toSummary(PokemonDto pokemon, String category) {
		return new PokemonSummary(
				pokemon.id(),
				pokemon.name(),
				sprite(pokemon),
				category,
				pokemon.weight(),
				pokemon.height(),
				types(pokemon),
				abilities(pokemon));
	}

	static PokemonDetail toDetail(PokemonDto pokemon, String category, String description, List<EvolutionStage> evolution) {
		return new PokemonDetail(
				pokemon.id(),
				pokemon.name(),
				artwork(pokemon).orElseGet(() -> sprite(pokemon)),
				category,
				pokemon.weight(),
				pokemon.height(),
				types(pokemon),
				abilities(pokemon),
				stats(pokemon),
				description,
				evolution);
	}

	static Optional<String> englishGenus(SpeciesDto species) {
		return orEmpty(species.genera()).stream()
				.filter(genus -> isNamed(genus.language(), ENGLISH.code()))
				.map(SpeciesDto.Genus::genus)
				.findFirst();
	}

	// PokeAPI lists flavor texts oldest game first, so the last English entry is the most recent one.
	static Optional<String> latestEnglishFlavorText(SpeciesDto species) {
		return orEmpty(species.flavorTextEntries()).stream()
				.filter(entry -> isNamed(entry.language(), ENGLISH.code()))
				.map(SpeciesDto.FlavorText::text)
				.reduce((first, second) -> second);
	}

	private static String sprite(PokemonDto pokemon) {
		return pokemon.sprites() == null ? null : pokemon.sprites().frontDefault();
	}

	private static Optional<String> artwork(PokemonDto pokemon) {
		return Optional.ofNullable(pokemon.sprites())
				.map(PokemonDto.Sprites::other)
				.map(PokemonDto.OtherSprites::officialArtwork)
				.map(PokemonDto.Artwork::frontDefault);
	}

	private static List<String> types(PokemonDto pokemon) {
		return orEmpty(pokemon.types()).stream()
				.sorted(Comparator.comparingInt(PokemonDto.TypeSlot::slot))
				.map(slot -> slot.type().name())
				.toList();
	}

	private static List<Ability> abilities(PokemonDto pokemon) {
		return orEmpty(pokemon.abilities()).stream()
				.sorted(Comparator.comparingInt(PokemonDto.AbilitySlot::slot))
				.map(slot -> new Ability(slot.ability().name(), slot.hidden()))
				.toList();
	}

	private static List<Stat> stats(PokemonDto pokemon) {
		return orEmpty(pokemon.stats()).stream()
				.map(stat -> new Stat(stat.stat().name(), stat.baseStat()))
				.toList();
	}
}
