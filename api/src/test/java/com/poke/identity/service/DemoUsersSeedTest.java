package com.poke.identity.service;

import com.poke.TestcontainersConfiguration;
import com.poke.identity.domain.InvalidCredentialsException;
import com.poke.identity.domain.RawPassword;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoUsersSeedTest {

	@Autowired
	AuthService authService;

	@Test
	void theDemoUsersCanLogInWithTheDocumentedPasswords() {
		assertThat(authService.login("admin", new RawPassword("Admin123!")).value()).isNotBlank();
		assertThat(authService.login("ash", new RawPassword("Pikachu123!")).value()).isNotBlank();
	}

	@Test
	void theDemoUsersRejectOtherPasswords() {
		assertThatThrownBy(() -> authService.login("ash", new RawPassword("Admin123!")))
			.isInstanceOf(InvalidCredentialsException.class);
	}
}
