package com.scorenow.scorenow_api.domain.chat.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import com.scorenow.scorenow_api.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_message_reports", uniqueConstraints = @UniqueConstraint(name = "uk_chat_report_user", columnNames = {"message_id", "reporter_id"}),
       indexes = @Index(name = "idx_chat_report_message", columnList = "message_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatMessage message;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User reporter;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    public ChatMessageReport(ChatMessage message, User reporter, String reason) {
        this.message = message; this.reporter = reporter; this.reason = reason; this.createdAt = LocalDateTime.now();
    }
}
