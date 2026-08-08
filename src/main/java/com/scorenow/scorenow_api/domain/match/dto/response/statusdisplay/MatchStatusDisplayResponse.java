package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "앱 경기 상태 표시 응답")
public class MatchStatusDisplayResponse {
    @Schema(description = "앱 표시용 경기 상태/시간/결과 문구", example = "후반 15")
    private String displayText;

    @Schema(
            description = """
                    상태별 추가 표시 정보입니다.
                    경기전/연기/취소 등은 null일 수 있습니다.
                    축구 진행중이면 FootballInplayStatusDisplayDetailResponse 형태,
                    경기종료이면 EndedStatusDisplayDetailResponse 형태로 내려갑니다.
                    """
    )
    private Object detail;

    public static MatchStatusDisplayResponse empty() {
        return MatchStatusDisplayResponse.builder().build();
    }
}
