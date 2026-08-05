package com.scorenow.scorenow_api.domain.match.document;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.global.entity.BaseDocument;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Document(collection = "match_details")
public class MatchDetailDocument extends BaseDocument {
    @Id
    private Long id;

    private SportDetailType type; //  종목

    private Integer homeScore;  // 홈팀 점수
    private Integer awayScore;  // 어웨이팀 점수

    private String currentCommentaryId; // 현재 중계 멘트 id
    private String currentCommentary;   // 현재 중계 멘트
    private boolean currentCommentaryHighlighted;   // 현재 중계 멘트 강조 여부

    private SportDetail sportDetail;    // 종목 별 상세 정보

    public void updateCurrentCommentary(String content, String commentaryId, boolean highlighted) {
        this.currentCommentary = content;
        this.currentCommentaryId = commentaryId;
        this.currentCommentaryHighlighted = highlighted;
    }

}