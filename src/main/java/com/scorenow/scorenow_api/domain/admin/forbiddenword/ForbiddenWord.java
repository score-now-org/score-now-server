package com.scorenow.scorenow_api.domain.admin.forbiddenword;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "forbidden_words", uniqueConstraints = @UniqueConstraint(name = "uk_forbidden_word", columnNames = "word"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ForbiddenWord extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String word;
    @Column(nullable = false, length = 100)
    private String replacement;

    public ForbiddenWord(String word, String replacement) {
        this.word = word;
        this.replacement = replacement;
    }
}
