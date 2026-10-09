package com.poke.catalog.client;

import com.poke.catalog.client.dto.EvolutionChainDto;
import com.poke.catalog.client.dto.EvolutionChainDto.EvolutionDetail;
import com.poke.catalog.client.dto.NamedResource;
import com.poke.catalog.domain.EvolutionStage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static com.poke.catalog.client.PokeApiFixtures.fixture;
import static org.assertj.core.api.Assertions.assertThat;

class EvolutionChainFlattenerTest {

	private final JsonMapper json = JsonMapper.builder().build();

	@Test
	void flattensALinearChainInStageOrder() throws IOException {
		var stages = EvolutionChainFlattener.flatten(chain("evolution-chain-10.json").chain());

		assertThat(stages).extracting(EvolutionStage::name).containsExactly("pichu", "pikachu", "raichu");
		assertThat(stages).extracting(EvolutionStage::stage).containsExactly(0, 1, 2);
		assertThat(stages.get(1).evolvesFromId()).isEqualTo(172);
		assertThat(stages.get(2).evolvesFromId()).isEqualTo(25);
		assertThat(stages.get(2).trigger()).isEqualTo("use-item: thunder-stone");
		assertThat(stages.getFirst().trigger()).isNull();
	}

	@Test
	void keepsBranchesAsSiblingsOfTheSameStage() throws IOException {
		var stages = EvolutionChainFlattener.flatten(chain("evolution-chain-67.json").chain());

		assertThat(stages.getFirst().name()).isEqualTo("eevee");
		assertThat(stages).hasSize(9);
		assertThat(stages.subList(1, 9)).allSatisfy(stage -> {
			assertThat(stage.stage()).isEqualTo(1);
			assertThat(stage.evolvesFromId()).isEqualTo(133);
		});
		assertThat(stages).extracting(EvolutionStage::name)
			.contains("vaporeon", "jolteon", "flareon", "espeon", "umbreon", "leafeon", "glaceon", "sylveon");
		assertThat(stages.get(1).trigger()).isEqualTo("use-item: water-stone");
	}

	@Test
	void buildsSpriteUrlsFromSpeciesIds() throws IOException {
		var stages = EvolutionChainFlattener.flatten(chain("evolution-chain-10.json").chain());

		assertThat(stages.get(1).spriteUrl())
			.isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png");
	}

	@Test
	void missingChainIsEmpty() {
		assertThat(EvolutionChainFlattener.flatten(null)).isEmpty();
	}

	@Test
	void describesTriggersByItemThenLevelThenTriggerName() {
		var levelUp = new NamedResource("level-up", null);
		var stone = new NamedResource("water-stone", null);

		assertThat(EvolutionChainFlattener.describeTrigger(List.of(new EvolutionDetail(levelUp, null, 16))))
			.isEqualTo("level-up: 16");
		assertThat(EvolutionChainFlattener.describeTrigger(List.of(new EvolutionDetail(new NamedResource("use-item", null), stone, null))))
			.isEqualTo("use-item: water-stone");
		assertThat(EvolutionChainFlattener.describeTrigger(List.of(new EvolutionDetail(levelUp, null, null))))
			.isEqualTo("level-up");
		assertThat(EvolutionChainFlattener.describeTrigger(List.of())).isNull();
	}

	private EvolutionChainDto chain(String name) throws IOException {
		return json.readValue(fixture(name), EvolutionChainDto.class);
	}
}
