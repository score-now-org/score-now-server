package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "관리자 리그 생성 요청")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminLeagueCreateRequest {

    @NotNull(message = "종목ID 는 필수입니다.")
    @Schema(description = "종목 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sportId;

    @NotBlank
    @Schema(description = "영문 리그명", example = "Premier League", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("eName")
    private String eName;

    @Schema(description = "한글 리그명", example = "프리미어리그")
    @JsonProperty("kName")
    private String kName;

    @NotBlank
    @Schema(description = "숏 리그명", example = "EPL", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("sName")
    private String sName;

    @Schema(description = "팀 표시 순서", example = "HOME_AWAY", allowableValues = {"HOME_AWAY", "AWAY_HOME"})
    private TeamDisplayOrder teamDisplayOrder;

    @NotNull(message = "외부 연동 여부는 필수입니다.")
    @Schema(description = "외부 API 연동 여부. false이면 내부 전용 리그로 등록합니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean externalLinked;

    @Schema(description = "데이터 원천. 외부 API 연동 리그이면 MANUAL이 아닌 값을 사용합니다.", example = "BETS", allowableValues = {"BETS", "MANUAL"})
    private DataOrigin dataOrigin;

    @Schema(description = "외부 API 리그 ID. 외부 API 연동 리그이면 필수입니다.", example = "94")
    private String apiLeagueId;

    @Schema(description = "외부 API 경기 동기화 활성화 여부. null이면 false로 처리됩니다.", example = "true")
    private Boolean syncEnabled;
}
