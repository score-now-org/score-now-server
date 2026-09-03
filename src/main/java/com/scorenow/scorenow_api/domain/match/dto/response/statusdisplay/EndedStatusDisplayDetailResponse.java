package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "경기 종료 상태 표시 상세 응답")
public class EndedStatusDisplayDetailResponse {
    @Schema(description = "경기 결과", example = "HOME_WIN", allowableValues = {"HOME_WIN", "AWAY_WIN", "DRAW", "UNKNOWN"})
    private String result;

    @Schema(description = "승리팀 ID. 무승부 또는 결과 미확정이면 null", example = "10")
    private Long winnerTeamId;

    @Schema(description = "승리팀 이름. 무승부 또는 결과 미확정이면 null", example = "대한민국")
    private String winnerTeamName;
}
