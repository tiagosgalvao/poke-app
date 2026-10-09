package com.poke.identity.controller;

import com.poke.identity.controller.AuthResponses.TokenResponse;
import com.poke.identity.controller.AuthResponses.UserResponse;
import com.poke.identity.domain.AccessToken;
import com.poke.identity.domain.User;
import com.poke.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static com.poke.identity.controller.AuthResponses.BEARER;

@Mapper(config = MappingConfig.class)
interface AuthResponseMapper {

	UserResponse toUserResponse(User user);

	@Mapping(target = "accessToken", source = "value")
	@Mapping(target = "tokenType", constant = BEARER)
	TokenResponse toTokenResponse(AccessToken token);
}
