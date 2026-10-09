package com.poke.identity.repository;

import com.poke.TestcontainersConfiguration;
import com.poke.identity.domain.User;
import com.poke.identity.entity.UserEntityMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({TestcontainersConfiguration.class, JpaUserRepository.class, UserEntityMapperImpl.class})
class JpaUserRepositoryTest {

	private static final Instant REGISTERED_AT = Instant.parse("2026-10-08T10:00:00Z");

	@Autowired
	JpaUserRepository repository;

	@Autowired
	TestEntityManager entityManager;

	@Test
	void savesAndFindsAUserByUsername() {
		var misty = User.register("test-misty", "test-misty@cerulean.city", "$2a$10$hash", REGISTERED_AT);

		repository.save(misty);
		entityManager.clear();

		assertThat(repository.findByUsername("test-misty")).contains(misty);
		assertThat(repository.findByUsername("test-brock")).isEmpty();
	}

	@Test
	void reportsTakenUsernamesAndEmails() {
		repository.save(User.register("test-brock", "test-brock@pewter.city", "$2a$10$hash", REGISTERED_AT));

		assertThat(repository.existsByUsername("test-brock")).isTrue();
		assertThat(repository.existsByEmail("test-brock@pewter.city")).isTrue();
		assertThat(repository.existsByUsername("test-gary")).isFalse();
		assertThat(repository.existsByEmail("test-gary@pallet.town")).isFalse();
	}
}
