package com.scorenow.scorenow_api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "refreshToken:";

    // 저장
    public void save(Long userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(PREFIX + userId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }
    //조회
    public String find(Long userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }
    //삭제
    public void delete(Long userId) {
        redisTemplate.delete(PREFIX + userId);
    }
    //검증
    public boolean isValid(Long userId, String refreshToken) {
        String stored = find(userId);
        return stored != null && stored.equals(refreshToken);
    }

}
