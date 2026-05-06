package com.scorenow.scorenow_api.domain.match.document;

import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@Document(collection = "match_details")
public class MatchDetailDocument {
    @Id
    private Long id;

    private MatchStats homeStats; // 홈팀 지표 (경고, 퇴장, 슈팅 등)
    private MatchStats awayStats; // 어웨이팀 지표

    private ExtraTime extraTime;

    private String currentCommentary;   // 현재 보여야 하는 중계 멘트
    private String currentCommentaryId; // 현재 보여야 하는 중계 멘트의 id

    @Getter
    @Builder
    public static class ExtraTime {
        private Integer firstHalf;
        private Integer secondHalf;
        private Integer overTime;
    }

    public void updateFrom(MatchDetailUpdateRequest request) {
        if (request.getHomeStats() != null) this.homeStats = request.getHomeStats();
        if (request.getAwayStats() != null) this.awayStats = request.getAwayStats();
        if (request.getExtraTime() != null) this.extraTime = request.getExtraTime();
    }

    public void updateCurrentCommentary(String content, String commentaryId) {
        this.currentCommentary = content;
        this.currentCommentaryId = commentaryId;
    }
}