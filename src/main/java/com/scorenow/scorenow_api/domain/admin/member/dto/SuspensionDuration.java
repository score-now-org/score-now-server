package com.scorenow.scorenow_api.domain.admin.member.dto;

import java.time.LocalDateTime;

public enum SuspensionDuration {
    WEEK, MONTH;

    public LocalDateTime expiresAt(LocalDateTime start) {
        return this == WEEK ? start.plusWeeks(1) : start.plusMonths(1);
    }
}
