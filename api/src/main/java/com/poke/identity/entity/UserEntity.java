package com.poke.identity.entity;

import com.poke.identity.domain.User;
import jakarta.persistence.*;
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

	@Override
	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
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
