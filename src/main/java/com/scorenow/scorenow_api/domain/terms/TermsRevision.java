package com.scorenow.scorenow_api.domain.terms;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "terms_revisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String content;
    @Column(nullable = false) private LocalDateTime publishedAt;

    TermsRevision(String content) { this.content = content; this.publishedAt = LocalDateTime.now(); }
}
