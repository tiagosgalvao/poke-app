package com.poke.identity.domain;

public interface TokenIssuer {

	AccessToken issueFor(User user);
}
