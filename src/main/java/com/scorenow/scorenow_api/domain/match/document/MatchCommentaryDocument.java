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
    private String currentMatchTime;  // "17` 호날두 역시 잘하네요." 와 같이 중계 멘트를 등록한다하면 17과 같은 현재 인게임 내 시간 정보가 필요
    private String content;
    private String imageUrl;

    @Builder.Default
    private boolean visible = true;
}