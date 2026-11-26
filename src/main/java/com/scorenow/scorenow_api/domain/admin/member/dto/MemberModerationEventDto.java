package com.scorenow.scorenow_api.domain.admin.member.dto;

import java.time.LocalDateTime;
import com.scorenow.scorenow_api.domain.admin.member.entity.MemberModerationEvent;

public record MemberModerationEventDto(Long id, String action, String reason, LocalDateTime createdAt, String duration) {
    public static MemberModerationEventDto from(MemberModerationEvent event) {
        return new MemberModerationEventDto(event.getId(), event.getAction(), event.getReason(), event.getCreatedAt(), event.getDuration());
    }
}
