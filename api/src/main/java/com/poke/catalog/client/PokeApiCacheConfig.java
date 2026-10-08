package com.poke.catalog.client;

import com.poke.catalog.client.dto.EvolutionChainDto;
import com.poke.catalog.client.dto.PokemonDto;
import com.poke.catalog.client.dto.PokemonListDto;
import com.poke.catalog.client.dto.SpeciesDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

import static com.poke.catalog.client.PokeApiCacheNames.EVOLUTION_CHAIN;
import static com.poke.catalog.client.PokeApiCacheNames.POKEMON;
import static com.poke.catalog.client.PokeApiCacheNames.POKEMON_PAGE;
import static com.poke.catalog.client.PokeApiCacheNames.SPECIES;

@Configuration(proxyBeanMethods = false)
@EnableCaching
class PokeApiCacheConfig implements CachingConfigurer {

	private static final String KEY_PREFIX = "poke::";
	private static final boolean LOG_STACK_TRACES = true;

	@Bean
	RedisCacheManager cacheManager(
			RedisConnectionFactory connectionFactory,
			@Value("${spring.cache.redis.time-to-live:24h}") Duration timeToLive) {
		var defaults = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(timeToLive)
				.prefixCacheNameWith(KEY_PREFIX);
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaults)
				.withInitialCacheConfigurations(Map.of(
						POKEMON_PAGE, typed(defaults, PokemonListDto.class),
						POKEMON, typed(defaults, PokemonDto.class),
						SPECIES, typed(defaults, SpeciesDto.class),
						EVOLUTION_CHAIN, typed(defaults, EvolutionChainDto.class)))
				.disableCreateOnMissingCache()
				.build();
	}

	@Override
	public CacheErrorHandler errorHandler() {
		return new LoggingCacheErrorHandler(LOG_STACK_TRACES);
	}

	private static RedisCacheConfiguration typed(RedisCacheConfiguration defaults, Class<?> type) {
		return defaults.serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(type)));
	}
}
