package com.scorenow.scorenow_api.domain.match.document;

import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@Document(collection = "match_details")
public class MatchDetailDocument {
    @Id
    private Long id;

    private Integer homeScore;  // 홈팀 점수
    private Integer awayScore;  // 어웨이팀 점수

    private MatchStats homeStats; // 홈팀 지표 (경고, 퇴장, 슈팅 등)
    private MatchStats awayStats; // 어웨이팀 지표

    private MatchClock matchClock;          // 경기 시간 정보
    private AdditionalTime additionalTime;  // 추가시간 정보

    private String currentCommentary;   // 현재 보여야 하는 중계 멘트
    private String currentCommentaryId; // 현재 보여야 하는 중계 멘트의 id

    @Getter
    @Builder
    public static class MatchClock {
        private String startAt;      // 경기 시작 시간 (MongoDB 의 경우 UTC 로 변환하여 저장하기 때문에 String 으로 저장)

        private Integer elapsedMinutes;     // 경과 시간 (분)
        private Integer elapsedSeconds;     // 경과 시간 (초)
        private MatchPeriod period;         // 전반, 후반
        private Boolean running;            // 시간 흐르는 여부

        private Instant providerUpdatedAt;  // 외부 API 에서 데이터를 업데이트 한 시점
    }

    @Getter
    @Builder
    public static class AdditionalTime {
        private Integer firstHalf;          // 전반 추가시간
        private Integer secondHalf;         // 후반 추가시간
        private Integer extraFirstHalf;     // 연장 전반 추가시간
        private Integer extraSecondHalf;    // 연장 후반 추가시간
    }

    public void updateFrom(MatchDetailUpdateRequest request) {
        if (request.getHomeScore() != null) this.homeScore = request.getHomeScore();
        if (request.getAwayScore() != null) this.awayScore = request.getAwayScore();
        if (request.getHomeStats() != null) this.homeStats = request.getHomeStats();
        if (request.getAwayStats() != null) this.awayStats = request.getAwayStats();
        if (request.getMatchClock() != null) this.matchClock = request.getMatchClock();
        if (request.getAdditionalTime() != null) this.additionalTime = request.getAdditionalTime();
    }

    public void updateCurrentCommentary(String content, String commentaryId) {
        this.currentCommentary = content;
        this.currentCommentaryId = commentaryId;
    }
}