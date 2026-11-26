package com.scorenow.scorenow_api.domain.admin.member.entity;

import java.time.LocalDateTime;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.admin.member.dto.SuspensionDuration;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_moderation_events", indexes = @Index(name = "idx_member_moderation_user", columnList = "user_id, id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberModerationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 32)
    private String action;
    @Column(length = 500)
    private String reason;
    @Column(length = 16)
    private String duration;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    public MemberModerationEvent(User user, String action, String reason, SuspensionDuration duration) {
        this.user = user;
        this.action = action;
        this.reason = reason;
        this.duration = duration == null ? null : duration.name();
        this.createdAt = LocalDateTime.now();
    }
}
