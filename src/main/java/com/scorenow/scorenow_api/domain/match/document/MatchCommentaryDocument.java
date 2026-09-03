package com.scorenow.scorenow_api.domain.match.document;

import com.scorenow.scorenow_api.global.entity.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "match_commentaries")
public class MatchCommentaryDocument extends BaseDocument {
    @Id
    private String id;

    @Indexed
    private Long matchId;

    private String content;

    @Builder.Default
    private boolean highlighted = false;

    private String imageUrl;

    @Builder.Default
    private boolean visible = true;
}