package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "상단고정/핫매치 등록 후보 경기 응답")
public class FeaturedMatchCandidateResponse {
    @Schema(description = "경기 ID", example = "1001")
    private final Long matchId;

    @Schema(description = "종목 ID", example = "1")
    private final Long sportId;

    @Schema(description = "종목명", example = "축구")
    private final String sportName;

    @Schema(description = "리그명", example = "프리미어리그")
    private final String leagueName;

    @Schema(description = "경기 예정 일시. KST 기준 yyyy-MM-dd'T'HH:mm:ss 형식입니다.", example = "2026-07-23T20:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime startAt;

    @Schema(description = "홈팀명", example = "토트넘")
    private final String homeTeamName;

    @Schema(description = "원정팀명", example = "아스널")
    private final String awayTeamName;

    @Schema(description = "상단고정/핫매치 신규 등록 가능 여부. 이미 등록된 경기이면 false입니다.", example = "true")
    private final boolean registerable;

    public FeaturedMatchCandidateResponse(
            Long matchId,
            Long sportId,
            String sportName,
            String leagueName,
            LocalDateTime startAt,
            String homeTeamName,
            String awayTeamName,
            boolean registerable) {

        this.matchId = matchId;
        this.sportId = sportId;
        this.sportName = sportName;
        this.leagueName = leagueName;
        this.startAt = startAt;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.registerable = registerable;
    }

}
