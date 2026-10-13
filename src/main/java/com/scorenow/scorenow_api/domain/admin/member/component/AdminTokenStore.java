package com.scorenow.scorenow_api.domain.admin.member.component;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Component
public class AdminTokenStore {
    private static final String PREFIX = "adminToken:";
    private final StringRedisTemplate redisTemplate;
    private final Duration tokenTtl;

    public AdminTokenStore(StringRedisTemplate redisTemplate,
            @Value("${admin.token-ttl-seconds:28800}") long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("관리자 토큰 만료 시간은 양수여야 합니다.");
        }
        this.redisTemplate = redisTemplate;
        this.tokenTtl = Duration.ofSeconds(ttlSeconds);
    }

    public void add(String token) {
        redisTemplate.opsForValue().set(PREFIX + token, "admin", tokenTtl);
    }

    public boolean isValid(String token) {
        return token != null && Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }

    public void delete(String token) {
        if (token != null) {
            redisTemplate.delete(PREFIX + token);
        }
    }

    public static String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim() : authorization.trim();
        try {
            return token.length() == 36 && UUID.fromString(token).toString().equals(token) ? token : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
