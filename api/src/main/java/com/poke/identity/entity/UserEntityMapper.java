package com.poke.identity.entity;

import com.poke.identity.domain.User;
import com.poke.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public interface UserEntityMapper {

	User toDomain(UserEntity entity);
}
