package com.scorenow.scorenow_api.domain.match.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class FootballMatchDetailUpdateRequest {

    private Integer homeScore;
    private Integer awayScore;

    private MatchStatus status;

    /* 승부차기 관련 */
    private Integer homeShootOutScore;
    private Integer awayShootOutScore;

    /* 스텟 관련 */
    private FootballStats homeStats;
    private FootballStats awayStats;

    /* 시간 관련 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
    private FootballPhase phase;

    /* 추가 시간 관련 */
    private Integer firstHalf;
    private Integer secondHalf;
    private Integer extraFirstHalf;
    private Integer extraSecondHalf;


    @Getter
    @NoArgsConstructor
    public static class FootballStats {
        private Integer yellowCards;
        private Integer redCards;
        private Integer shots;
        private Integer shotsOnTarget;
        private Integer possession;
        private Integer offsides;
        private Integer fouls;
        private Integer corners;
        private Integer freeKicks;
    }

}