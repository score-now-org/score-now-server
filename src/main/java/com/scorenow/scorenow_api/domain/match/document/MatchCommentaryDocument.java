package com.scorenow.scorenow_api.domain.match.document;

import com.scorenow.scorenow_api.global.entity.BaseDocument;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Builder
@Document(collection = "match_commentaries")
public class MatchCommentaryDocument extends BaseDocument {
    @Id
    private String id;
    private String eventId;
    private String minute;
    private String content;
    private String imageUrl;

}