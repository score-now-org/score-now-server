package com.scorenow.scorenow_api.global.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

	/**
	 * RedisTemplate 설정
	 * - Key: String (StringRedisSerializer)
	 * - Value: JSON (GenericJackson2JsonRedisSerializer)
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key는 String으로 직렬화
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		// Value는 JSON으로 직렬화
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
		template.setValueSerializer(serializer);
		template.setHashValueSerializer(serializer);

		return template;
	}

	/**
	 * CacheManager 설정
	 * - TTL: 10분
	 * - JSON: 직렬화
	 */
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory){
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
			.serializeKeysWith(
				RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
			)
			.serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(
					new GenericJackson2JsonRedisSerializer()
				)
			);

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(config)
			.build();
	}
}
