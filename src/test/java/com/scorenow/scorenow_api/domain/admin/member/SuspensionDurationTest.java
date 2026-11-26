package com.scorenow.scorenow_api.domain.admin.member;

import com.scorenow.scorenow_api.domain.admin.member.dto.SuspensionDuration;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SuspensionDurationTest {
    @Test
    void monthUsesKoreanCalendarAcrossUtcMonthBoundary() {
        // March 1 at 00:30 in Korea is still February 28 in UTC.
        LocalDateTime startUtc = LocalDateTime.of(2026, 2, 28, 15, 30);
        assertThat(SuspensionDuration.MONTH.expiresAt(startUtc))
                .isEqualTo(LocalDateTime.of(2026, 3, 31, 15, 30));
    }
}
