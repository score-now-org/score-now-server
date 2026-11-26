package com.scorenow.scorenow_api.domain.admin.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@EnabledIfEnvironmentVariable(named = "ADMIN_REDIS_DISPOSABLE_TEST", matches = "true")
class AdminTokenRedisIntegrationTest {
    @Test void sessionsAreSharedExpireAndCanBeRevoked() {
        LettuceConnectionFactory connection = new LettuceConnectionFactory("127.0.0.1", 16379);
        connection.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(connection);
        redis.afterPropertiesSet();
        String token = UUID.randomUUID().toString();
        AdminTokenStore first = new AdminTokenStore(redis, 60);
        AdminTokenStore second = new AdminTokenStore(redis, 60);
        try {
            first.add(token);
            assertThat(second.isValid(token)).isTrue();
            assertThat(redis.getExpire("adminToken:" + token, TimeUnit.SECONDS)).isBetween(1L, 60L);
            second.delete(token);
            assertThat(first.isValid(token)).isFalse();
        } finally {
            first.delete(token);
            connection.destroy();
        }
    }
}
