package com.poke.identity.repository;

import com.poke.identity.domain.User;
import com.poke.identity.domain.UserRepository;
import com.poke.identity.entity.UserEntity;
import com.poke.identity.entity.UserEntityMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaUserRepository implements UserRepository {

	private final UserJpaRepository jpa;
	private final UserEntityMapper mapper;

	public JpaUserRepository(UserJpaRepository jpa, UserEntityMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return jpa.findByUsername(username).map(mapper::toDomain);
	}

	@Override
	public boolean existsByUsername(String username) {
		return jpa.existsByUsername(username);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpa.existsByEmail(email);
	}

	@Override
	@Transactional
	public User save(User user) {
		return mapper.toDomain(jpa.saveAndFlush(UserEntity.from(user)));
	}
}
