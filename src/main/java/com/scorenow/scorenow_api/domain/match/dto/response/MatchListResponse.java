package com.scorenow.scorenow_api.domain.match.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 경기 목록 응답")
public class MatchListResponse {
    @Schema(description = "경기 ID", example = "1001")
    private Long id;

    @Schema(description = "종목 ID", example = "1")
    private Long sportId;

    @Schema(description = "종목명", example = "축구")
    private String sportName;

    @Schema(description = "리그 ID", example = "2")
    private Long leagueId;

    @Schema(description = "리그명", example = "프리미어리그")
    private String leagueName;

    @Schema(description = "경기 타입", example = "리그")
    private String matchType;

    @Schema(description = "경기 상태 코드", example = "IN_PLAY")
    private String statusCode;

    @Schema(description = "경기 상태명", example = "진행중")
    private String statusName;

    @Schema(description = "홈팀 ID", example = "10")
    private Long homeId;

    @Schema(description = "홈팀명", example = "토트넘")
    private String homeName;

    @Schema(description = "홈팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/10.png")
    private String homeImageUrl;

    @Schema(description = "홈팀 점수", example = "1")
    private Integer homeScore;

    @Schema(description = "원정팀 ID", example = "20")
    private Long awayId;

    @Schema(description = "원정팀명", example = "아스널")
    private String awayName;

    @Schema(description = "원정팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/20.png")
    private String awayImageUrl;

    @Schema(description = "원정팀 점수", example = "0")
    private Integer awayScore;

    @Schema(description = "수동 등록 경기 여부. true이면 수동, false이면 자동", example = "true")
    private boolean isManual;

    @Schema(description = "앱 노출 여부", example = "true")
    private boolean isActive;

    @Schema(description = "팀 표시 순서", example = "[\"HOME\", \"AWAY\"]")
    private List<String> teamDisplayOrder;

    @Schema(description = "경기 시작 시간", example = "2026-06-05T20:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
}
