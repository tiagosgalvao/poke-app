package com.poke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationTest {

	private static final String INSERT_POKEMON = """
			insert into local_pokemon (id, name, weight_hectograms, height_decimetres, synced_at, updated_at)
			values (?, ?, ?, ?, now(), now())""";
	private static final String INSERT_USER = "insert into users (id, username, email, password_hash) values (gen_random_uuid(), ?, ?, 'hash')";
	private static final int FIRST_TEST_ID = 901;
	private static final int SECOND_TEST_ID = 902;
	private static final int SEEDED_POKEMON = 20;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void removeTestData() {
		jdbc.update("delete from local_pokemon where id >= ?", FIRST_TEST_ID);
		jdbc.update("delete from users where username like 'test-%'");
	}

	@Test
	void createsTheUsersAndLocalPokemonTables() {
		var tables = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public'", String.class);

		assertThat(tables).contains("users", "local_pokemon", "local_pokemon_type", "local_pokemon_ability", "local_pokemon_tag");
	}

	@Test
	void newPokemonStartAtVersionZeroWithNoProprietaryData() {
		jdbc.update(INSERT_POKEMON, FIRST_TEST_ID, "test-pokemon-a", 60, 4);

		var row = jdbc.queryForMap("select version, localized_name, region, habitat, notes from local_pokemon where id = " + FIRST_TEST_ID);

		assertThat(row.get("version")).isEqualTo(0L);
		assertThat(row.get("localized_name")).isNull();
		assertThat(row.get("region")).isNull();
		assertThat(row.get("habitat")).isNull();
		assertThat(row.get("notes")).isNull();
	}

	@Test
	void pokemonNamesAreUnique() {
		jdbc.update(INSERT_POKEMON, FIRST_TEST_ID, "test-pokemon-a", 60, 4);

		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, SECOND_TEST_ID, "test-pokemon-a", 300, 8))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsNonPositiveIdsAndNegativeMeasures() {
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, 0, "test-missingno", 1, 1))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, SECOND_TEST_ID, "test-pokemon-b", -1, 7))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, SECOND_TEST_ID, "test-pokemon-b", 69, -1))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingAPokemonRemovesItsTypesAbilitiesAndTags() {
		jdbc.update(INSERT_POKEMON, FIRST_TEST_ID, "test-pokemon-a", 60, 4);
		jdbc.update("insert into local_pokemon_type (pokemon_id, position, type) values (?, 0, 'electric')", FIRST_TEST_ID);
		jdbc.update("insert into local_pokemon_ability (pokemon_id, position, ability) values (?, 0, 'static')", FIRST_TEST_ID);
		jdbc.update("insert into local_pokemon_tag (pokemon_id, tag) values (?, 'starter')", FIRST_TEST_ID);

		jdbc.update("delete from local_pokemon where id = ?", FIRST_TEST_ID);

		assertThat(countFor("local_pokemon_type")).isZero();
		assertThat(countFor("local_pokemon_ability")).isZero();
		assertThat(countFor("local_pokemon_tag")).isZero();
	}

	@Test
	void collectionsRequireAnExistingPokemon() {
		assertThatThrownBy(() -> jdbc.update("insert into local_pokemon_tag (pokemon_id, tag) values (?, 'orphan')", SECOND_TEST_ID))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void usernamesAndEmailsAreUnique() {
		jdbc.update(INSERT_USER, "test-ash", "test-ash@pallet.town");

		assertThatThrownBy(() -> jdbc.update(INSERT_USER, "test-ash", "test-other@pallet.town"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_USER, "test-misty", "test-ash@pallet.town"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void seedsTwentyKantoPokemonForTheDemo() {
		var seeded = jdbc.queryForObject("select count(*) from local_pokemon where id < ?", Integer.class, FIRST_TEST_ID);

		assertThat(seeded).isEqualTo(SEEDED_POKEMON);
	}

	@Test
	void seededPokemonCarryUpstreamAndProprietaryData() {
		var pikachu = jdbc.queryForMap(
				"select name, category, weight_hectograms, localized_name, region, habitat, notes from local_pokemon where id = 25");
		var types = jdbc.queryForList("select type from local_pokemon_type where pokemon_id = 25 order by position", String.class);
		var abilities = jdbc.queryForList(
				"select ability from local_pokemon_ability where pokemon_id = 25 order by position", String.class);
		var tags = jdbc.queryForList("select tag from local_pokemon_tag where pokemon_id = 25", String.class);

		assertThat(pikachu).containsEntry("name", "pikachu")
				.containsEntry("weight_hectograms", 60)
				.containsEntry("localized_name", "ピカチュウ")
				.containsEntry("region", "Kanto")
				.containsEntry("habitat", "forest");
		assertThat(pikachu.get("notes")).isNotNull();
		assertThat(types).containsExactly("electric");
		assertThat(abilities).containsExactly("static", "lightning-rod");
		assertThat(tags).containsExactly("mascot");
	}

	@Test
	void someSeededPokemonHaveNoProprietaryDataYet() {
		var raichu = jdbc.queryForMap("select localized_name, region from local_pokemon where id = 26");

		assertThat(raichu.get("localized_name")).isNull();
		assertThat(raichu.get("region")).isNull();
	}

	private int countFor(String table) {
		return jdbc.queryForObject("select count(*) from " + table + " where pokemon_id = ?", Integer.class, FIRST_TEST_ID);
	}
}
