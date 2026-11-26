package com.scorenow.scorenow_api.domain.admin.member.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public enum SuspensionDuration {
    WEEK, MONTH;

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    public LocalDateTime expiresAt(LocalDateTime start) {
        if (this == WEEK) return start.plusWeeks(1);
        return start.atZone(ZoneOffset.UTC).withZoneSameInstant(KOREA)
                .plusMonths(1).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
