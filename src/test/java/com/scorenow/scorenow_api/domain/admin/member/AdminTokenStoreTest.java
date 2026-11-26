package com.scorenow.scorenow_api.domain.admin.member;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.scorenow.scorenow_api.domain.admin.member.component.AdminTokenStore;

class AdminTokenStoreTest {
    @Test void storesSessionsWithExpiryAndParsesOnlyAdminTokens() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AdminTokenStore store = new AdminTokenStore(redis, 28800);
        String token = "ca993c42-9e2f-4e91-a272-3f34ad1024c2";
        store.add(token);
        verify(values).set("adminToken:" + token, "admin", Duration.ofHours(8));
        assertThat(AdminTokenStore.resolveToken("Bearer " + token)).isEqualTo(token);
        assertThat(AdminTokenStore.resolveToken("  bEaReR " + token.toUpperCase() + "  ")).isEqualTo(token);
        assertThat(AdminTokenStore.resolveToken(token)).isEqualTo(token);
        assertThat(AdminTokenStore.resolveToken("Bearer user.jwt.token")).isNull();
        assertThat(AdminTokenStore.resolveToken("Bearer 1-1-1-1-1")).isNull();
        assertThat(store.isValid(null)).isFalse();
        store.delete(token);
        verify(redis).delete("adminToken:" + token);
    }
}
