package com.scorenow.scorenow_api.domain.match.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 축구 경기 상세 수정 요청. null 필드는 변경하지 않습니다.")
public class FootballMatchDetailUpdateRequest {

    @Schema(description = "홈팀 점수. null이면 변경하지 않습니다.", example = "1")
    private Integer homeScore;

    @Schema(description = "원정팀 점수. null이면 변경하지 않습니다.", example = "0")
    private Integer awayScore;

    @Schema(
            description = "경기 상태. null이면 변경하지 않습니다.",
            example = "IN_PLAY",
            allowableValues = {
                    "NOT_STARTED", "IN_PLAY", "TO_BE_FIXED", "ENDED", "POSTPONED",
                    "CANCELLED", "WALKOVER", "INTERRUPTED", "ABANDONED", "RETIRED", "REMOVED"
            }
    )
    private MatchStatus status;

    @Schema(description = "홈팀 승부차기 점수. null이면 변경하지 않습니다.", example = "4")
    private Integer homeShootOutScore;

    @Schema(description = "원정팀 승부차기 점수. null이면 변경하지 않습니다.", example = "3")
    private Integer awayShootOutScore;

    @Schema(description = "홈팀 축구 스탯. null이면 변경하지 않습니다.")
    private FootballStats homeStats;

    @Schema(description = "원정팀 축구 스탯. null이면 변경하지 않습니다.")
    private FootballStats awayStats;

    @Schema(description = "경기 시작 시간. null이면 변경하지 않습니다.", example = "2026-06-05T20:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @Schema(
            description = "축구 경기 구간. null이면 변경하지 않습니다.",
            example = "SECOND_HALF",
            allowableValues = {
                    "FIRST_HALF", "HALF_TIME", "SECOND_HALF", "EXTRA_TIME_WAITING",
                    "EXTRA_FIRST_HALF", "EXTRA_SECOND_HALF", "EXTRA_TIME_ENDED",
                    "PENALTY_SHOOTOUT", "FULL_TIME"
            }
    )
    private FootballPhase phase;

    @Schema(description = "전반 추가시간. null이면 변경하지 않습니다.", example = "2")
    private Integer firstHalf;

    @Schema(description = "후반 추가시간. null이면 변경하지 않습니다.", example = "4")
    private Integer secondHalf;

    @Schema(description = "연장 전반 추가시간. null이면 변경하지 않습니다.", example = "1")
    private Integer extraFirstHalf;

    @Schema(description = "연장 후반 추가시간. null이면 변경하지 않습니다.", example = "1")
    private Integer extraSecondHalf;


    @Getter
    @NoArgsConstructor
    @Schema(description = "축구 경기 스탯")
    public static class FootballStats {
        @Schema(description = "경고 수", example = "2")
        private Integer yellowCards;

        @Schema(description = "퇴장 수", example = "0")
        private Integer redCards;

        @Schema(description = "슈팅 수", example = "12")
        private Integer shots;

        @Schema(description = "유효 슈팅 수", example = "5")
        private Integer shotsOnTarget;

        @Schema(description = "점유율", example = "55")
        private Integer possession;

        @Schema(description = "오프사이드 수", example = "1")
        private Integer offsides;

        @Schema(description = "파울 수", example = "8")
        private Integer fouls;

        @Schema(description = "코너킥 수", example = "6")
        private Integer corners;

        @Schema(description = "프리킥 수", example = "10")
        private Integer freeKicks;
    }

}
