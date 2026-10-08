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

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void removeTestData() {
		jdbc.update("truncate table users, local_pokemon cascade");
	}

	@Test
	void createsTheUsersAndLocalPokemonTables() {
		var tables = jdbc.queryForList(
				"select table_name from information_schema.tables where table_schema = 'public'", String.class);

		assertThat(tables).contains("users", "local_pokemon", "local_pokemon_type", "local_pokemon_ability", "local_pokemon_tag");
	}

	@Test
	void newPokemonStartAtVersionZeroWithNoProprietaryData() {
		jdbc.update(INSERT_POKEMON, 25, "pikachu", 60, 4);

		var row = jdbc.queryForMap("select version, localized_name, region, habitat, notes from local_pokemon where id = 25");

		assertThat(row.get("version")).isEqualTo(0L);
		assertThat(row.get("localized_name")).isNull();
		assertThat(row.get("region")).isNull();
		assertThat(row.get("habitat")).isNull();
		assertThat(row.get("notes")).isNull();
	}

	@Test
	void pokemonNamesAreUnique() {
		jdbc.update(INSERT_POKEMON, 25, "pikachu", 60, 4);

		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, 26, "pikachu", 300, 8))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsNonPositiveIdsAndNegativeMeasures() {
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, 0, "missingno", 1, 1))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, 1, "bulbasaur", -1, 7))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_POKEMON, 1, "bulbasaur", 69, -1))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingAPokemonRemovesItsTypesAbilitiesAndTags() {
		jdbc.update(INSERT_POKEMON, 25, "pikachu", 60, 4);
		jdbc.update("insert into local_pokemon_type (pokemon_id, position, type) values (25, 0, 'electric')");
		jdbc.update("insert into local_pokemon_ability (pokemon_id, position, ability) values (25, 0, 'static')");
		jdbc.update("insert into local_pokemon_tag (pokemon_id, tag) values (25, 'starter')");

		jdbc.update("delete from local_pokemon where id = 25");

		assertThat(count("local_pokemon_type")).isZero();
		assertThat(count("local_pokemon_ability")).isZero();
		assertThat(count("local_pokemon_tag")).isZero();
	}

	@Test
	void collectionsRequireAnExistingPokemon() {
		assertThatThrownBy(() -> jdbc.update("insert into local_pokemon_tag (pokemon_id, tag) values (999, 'orphan')"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void usernamesAndEmailsAreUnique() {
		jdbc.update(INSERT_USER, "ash", "ash@pallet.town");

		assertThatThrownBy(() -> jdbc.update(INSERT_USER, "ash", "other@pallet.town"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(INSERT_USER, "misty", "ash@pallet.town"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private int count(String table) {
		return jdbc.queryForObject("select count(*) from " + table, Integer.class);
	}
}
