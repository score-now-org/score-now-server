package com.scorenow.scorenow_api.domain.league.dto.request;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 리그 수정 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLeagueUpdateRequest {

	@Schema(description = "종목 ID. null이면 변경하지 않습니다.", example = "1")
	private Long sportId;

	@Schema(description = "영문 리그명. null이면 변경하지 않습니다.", example = "Premier League")
	private String eName;

	@Schema(description = "한글 리그명. null이면 변경하지 않습니다.", example = "프리미어리그")
	private String kName;

	@Schema(description = "숏 리그명. null이면 변경하지 않습니다.", example = "EPL")
	private String sName;

	@Schema(description = "팀 표시 순서. null이면 변경하지 않습니다.", example = "HOME_AWAY", allowableValues = {"HOME_AWAY", "AWAY_HOME"})
	private TeamDisplayOrder teamDisplayOrder;
}
