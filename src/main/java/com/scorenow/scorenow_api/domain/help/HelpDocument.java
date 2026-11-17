package com.scorenow.scorenow_api.domain.help;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "help_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HelpDocument {
    @Id
    private Long id;

    @Version
    private Long version;

    @Lob
    @Column(name = "draft_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String draftJson;

    @Lob
    @Column(name = "published_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String publishedJson;

    HelpDocument(String draftJson) {
        this.id = 1L;
        this.draftJson = draftJson;
        this.publishedJson = "[]";
    }

    void update(String draftJson) {
        this.draftJson = draftJson;
    }

    void publish() { this.publishedJson = this.draftJson; }
}
