package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 경기 수정 요청. null 필드는 변경하지 않습니다.")
public class MatchUpdateRequest {

	@Schema(
			description = "경기 상태 코드. null이면 변경하지 않습니다.",
			example = "IN_PLAY",
			allowableValues = {
					"NOT_STARTED", "IN_PLAY", "TO_BE_FIXED", "ENDED", "POSTPONED",
					"CANCELLED", "WALKOVER", "INTERRUPTED", "ABANDONED", "RETIRED", "REMOVED"
			}
	)
	private MatchStatus statusCode; // NOT_STARTED, IN_PLAY, ENDED

	@Schema(description = "홈팀 점수. null이면 변경하지 않습니다.", example = "1")
	private Integer homeScore;

	@Schema(description = "원정팀 점수. null이면 변경하지 않습니다.", example = "0")
	private Integer awayScore;

	@Schema(description = "자동/수동 경기 여부. null이면 변경하지 않습니다. true이면 수동, false이면 자동", example = "true")
	private Boolean isManual;

	@Schema(description = "앱 노출 여부. null이면 변경하지 않습니다.", example = "true")
	private Boolean isActive;

	@JsonProperty("teamDisplayOrder")
	@Schema(description = "팀 표시 순서. null이면 변경하지 않습니다.", example = "HOME_AWAY", allowableValues = {"HOME_AWAY", "AWAY_HOME"})
	private TeamDisplayOrder teamDisplayOrder;

	@Schema(description = "경기 시작 시간. null이면 변경하지 않습니다.", example = "2026-06-05T20:30:00")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
