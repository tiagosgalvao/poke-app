package com.poke.identity.entity;

import com.poke.identity.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity implements Persistable<UUID> {

	@Id
	private UUID id;

	private String username;
	private String email;
	private String passwordHash;
	private Instant createdAt;

	@Transient
	private boolean isNew;

	protected UserEntity() {
	}

	public static UserEntity from(User user) {
		var entity = new UserEntity();
		entity.id = user.id();
		entity.username = user.username();
		entity.email = user.email();
		entity.passwordHash = user.passwordHash();
		entity.createdAt = user.createdAt();
		entity.isNew = true;
		return entity;
	}

	public User toDomain() {
		return new User(id, username, email, passwordHash, createdAt);
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markPersisted() {
		isNew = false;
	}
}
