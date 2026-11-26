package com.scorenow.scorenow_api.domain.inquiry;

import java.time.LocalDateTime;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "inquiries", indexes = @Index(name = "idx_inquiries_user_id_id", columnList = "user_id,id"))
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 200) private String title;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Lob @Column(columnDefinition = "TEXT") private String answer;
    private LocalDateTime answeredAt;
    @Column(nullable = false) private boolean hasImage;

    public Inquiry(User user, String title, String content) {
        this.user = user; this.title = title.trim(); this.content = content.trim();
    }
    public void attachImage() { this.hasImage = true; }
    public void answer(String answer) { this.answer = answer.trim(); this.answeredAt = LocalDateTime.now(); }
}
